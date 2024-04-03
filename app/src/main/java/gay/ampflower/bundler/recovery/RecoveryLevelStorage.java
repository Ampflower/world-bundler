package gay.ampflower.bundler.recovery;

import gay.ampflower.bundler.nbt.NbtCompound;
import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.utils.pos.Pos2d;
import gay.ampflower.bundler.utils.pos.Pos2i;
import gay.ampflower.bundler.utils.pos.Pos3i;
import gay.ampflower.bundler.world.Chunk;
import gay.ampflower.bundler.world.Region;
import gay.ampflower.bundler.world.io.ChunkStorage;
import gay.ampflower.bundler.world.io.resolvers.FileResolvers;
import gay.ampflower.bundler.world.util.ChunkDataUtil;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class RecoveryLevelStorage {
	private static final Logger logger = LogUtils.logger();

	public final Path root, garbageDump;
	private final FileResolvers fileResolver;
	public final Map<String, RecoveryWorldStorage> worlds = new HashMap<>();
	public final RecoveryPlayerStorage advancements, stats, playerdata;
	public final List<String> datapacks = new ArrayList<>();

	public final RecoveryWorldStorage overworld, theNether, theEnd;

	// level.dat data
	public Datapacks datapackMeta;
	public DragonFight dragonFight;
	public final Map<String, String> gamerules = new HashMap<>();
	// Player is purposefully omitted
	public Version Version;
	public WorldGenSettings worldGenSettings;
	public final List<ScheduledEvent> scheduledEvents = new ArrayList<>();
	public final List<String> serverBrands = new ArrayList<>();

	public boolean allowCommands;
	public WorldBorder border;
	public Time time;
	public Difficulty difficulty;

	public boolean initialized;
	public long lastPlayed;
	public String levelName;
	public int version;
	public boolean wasModded;

	public float spawnAngle;
	public Pos3i spawn;

	public int WanderingTraderSpawnChance;
	public int WanderingTraderSpawnDelay;

	public RecoveryLevelStorage(Path root, FileResolvers fileResolver) throws IOException {
		this.root = root;
		this.fileResolver = fileResolver;

		this.garbageDump = root.resolve("garbage");

		// These are handled specially
		worlds.put("minecraft:overworld", this.overworld = new RecoveryWorldStorage(root, this.fileResolver).init());
		worlds.put("minecraft:the_end", this.theEnd = new RecoveryWorldStorage(root.resolve("DIM1"), this.fileResolver).init());
		worlds.put("minecraft:the_nether", this.theNether = new RecoveryWorldStorage(root.resolve("DIM-1"), this.fileResolver).init());

		playerdata = new RecoveryPlayerStorage(root.resolve("playerdata"), RecoveryPlayerStorage.Type.Nbt);
		advancements = new RecoveryPlayerStorage(root.resolve("advancements"), RecoveryPlayerStorage.Type.Json);
		stats = new RecoveryPlayerStorage(root.resolve("stats"), RecoveryPlayerStorage.Type.Json);

		taint();
	}

	/**
	 * Taint the metadata with recovery information
	 */
	public void taint() {
		serverBrands.add("Ampflower's World Bundler");
		Version = new Version(0, "Recovered", "unknown", true);
		wasModded = true;
	}

	public void recover(Region recoveredRegion) throws IOException {
		logger.trace("Raw: {}", recoveredRegion);
		if (recoveredRegion == null || (recoveredRegion.isEmpty() && recoveredRegion.meta() == null)) {
			logger.warn("Failed to read a region");
			return;
		}
		logger.info("Got [{},{}]: {}", recoveredRegion.x(), recoveredRegion.y(), recoveredRegion.popCount());

		final int x = recoveredRegion.x(), y = recoveredRegion.y();
		final var regions = mapChunkToRegion(recoveredRegion);

		for (final var entry : regions.entrySet()) {
			final var pos = entry.getKey();
			final var storage = overworld.regions;
			final var region = storage.readRegion(pos.x(), pos.y());
			final var chunks = entry.getValue();
			if (region == null) {
				logger.info("Directly saving recovery[{},{}] as is to overworld[{},{}]", x, y, pos.x(), pos.y());
				storage.writeRegion(pos.x(), pos.y(), new Region(pos.x(), pos.y(), chunks));
				continue;
			}
			final var swap = region.chunks();
			for (int i = 0; i < Region.CHUNK_COUNT; i++) {
				final var chunk = chunks[i];
				if (chunk == null) {
					continue;
				}

				final var swapChunk = swap[i];
				if (swapChunk == null || writeNewChunk(swapChunk, chunk, new Pos2i(x, y), pos, i)) {
					swap[i] = chunk;
				}
			}
			logger.info("Saving recovery[{},{}] into overworld[{},{}]", x, y, pos.x(), pos.y());
			storage.writeRegion(pos.x(), pos.y(), region);
		}
	}

	private static boolean writeNewChunk(final Chunk swapChunk, final Chunk recoveredChunk, final Pos2i origin,
													 final Pos2i destination, int i) {
		if (recoveredChunk == null || recoveredChunk.size() == 0) {
			return false;
		}

		if (swapChunk == null || swapChunk.size() == 0) {
			logger.info("Putting chunk @ recovery[{},{}][{}] in swap[{},{}] as is",
				origin.x(), origin.y(), i, destination.x(), destination.y());
			return true;
		}

		if (!Objects.equals(recoveredChunk.nbt(), swapChunk.nbt())) {
			logger.warn("Chunk @ region[{},{}][{}] mismatch swap[{},{}], doing invasive comparison",
				origin.x(), origin.y(), i, destination.x(), destination.y());

			if (recoveredChunk.timestamp() > swapChunk.timestamp()) {
				logger.info("Recovery is newer than swap ({} > {}), assuming okay",
					recoveredChunk.timestamp(), swapChunk.timestamp());
				return true;
			}
			logger.info("Swap wins.");
		}
		return false;
	}

	private static Map<Pos2i, Chunk[]> mapChunkToRegion(Region region) {
		final var map = new HashMap<Pos2i, Chunk[]>();

		for (int i = 0; i < Region.CHUNK_COUNT; i++) {
			final var chunk = region.chunks()[i];
			if (chunk == null) {
				continue;
			}
			final var pos = getPosition(chunk);
			if (pos == null) {
				continue;
			}
			final var regionPos = new Pos2i(pos.x() >> Region.BIT_SHIFT, pos.y() >> Region.BIT_SHIFT);

			map.computeIfAbsent(regionPos, $ -> new Chunk[Region.CHUNK_COUNT])[i] = chunk;
		}

		return map;
	}

	private static Pos2i getPosition(Chunk chunk) {
		return ChunkDataUtil.getPosition(chunk.nbt());
	}

	private ChunkStorage sniff(NbtCompound compound) {
		boolean hasX = compound.hasKey("xPos");
		boolean hasZ = compound.hasKey("zPos");

		if (hasX && hasZ) {
			return overworld.regions;
		}

		logger.warn("Unknown chunk type: {}", compound);

		return null;
	}

	private static boolean isMetaEmpty(Region region) {
		if (region == null || region.meta() == null) {
			return true;
		}
		if (region.meta() instanceof Collection<?> collection) {
			return collection.isEmpty();
		}
		if (region.meta() instanceof Map<?, ?> map) {
			return map.isEmpty();
		}
		return false;
	}

	public record Datapacks(
		String[] disabled,
		String[] enabled
	) {
	}

	public record DragonFight(
		boolean killed,
		boolean needsStateScanning,
		boolean previouslyKilled,
		int[] gateways
	) {
	}

	public record Version(
		int id,
		String name,
		String series,
		boolean snapshot
	) {
	}

	public record WorldGenSettings(
		Map<String, DimensionGenSettings> dimensions,
		boolean bonusChest,
		boolean generateFeatures,
		long seed
	) {
	}

	public record DimensionGenSettings(
		BiomeSource biomeSource,
		String generatorSettings,
		String generatorType,
		String dimensionType
	) {
	}

	public record BiomeSource(
		String preset,
		String type, // the_end uses this as preset?
		List<BiomeParameterBundle> biomes
	) {
	}

	public record BiomeParameterBundle(
		Map<String, float[]> parameters,
		float parameterOffset,
		String biome
	) {
	}

	public record ScheduledEvent(
		ScheduledEventCallback callback,
		String name,
		long triggerTime
	) {
	}

	public record ScheduledEventCallback(
		String name,
		String type
	) {
	}

	public record WorldBorder(
		Pos2d center,
		double damagePerBlock,
		double safeZone,
		double size,
		double sizeLerpTarget,
		long sizeLerpTime,
		double warningBlocks,
		double warningTtime
	) {
	}

	public record Time(
		boolean raining,
		boolean thundering,
		int clearWeatherTime,
		int rainTime,
		int thunderTime,
		long dayTime,
		long time
	) {
	}

	public record Difficulty(
		byte difficulty,
		boolean difficultyLocked,
		int gameType,
		boolean hardcore
	) {
	}
}
