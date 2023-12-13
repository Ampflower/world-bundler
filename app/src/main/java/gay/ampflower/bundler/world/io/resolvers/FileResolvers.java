package gay.ampflower.bundler.world.io.resolvers;

import gay.ampflower.bundler.utils.Constants;
import gay.ampflower.bundler.utils.LevelCompressor;
import gay.ampflower.bundler.utils.function.FileResolver;
import gay.ampflower.bundler.utils.pos.Pos2i;
import gay.ampflower.bundler.world.AlphaChunkStorage;
import gay.ampflower.bundler.world.RegionedChunkStorage;
import gay.ampflower.bundler.world.io.ChunkStorage;
import gay.ampflower.bundler.world.io.RegionHandler;
import gay.ampflower.bundler.world.region.LinearHandler;
import gay.ampflower.bundler.world.region.McRegionHandler;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @author Ampflower
 * @since ${version}
 **/
public enum FileResolvers {
	McRegion(Type.REGION, McRegionHandler.INSTANCE, ".mcr", ".mcc"),
	Anvil(Type.REGION, McRegionHandler.INSTANCE, ".mca", ".mcc"),
	Linear(Type.REGION, LinearHandler.INSTANCE, ".linear", null),
	Alpha(Chunk.inst, ".dat",
		Pattern.compile("/" + Constants.base36 + "{1,2}/" + Constants.base36 + "{1,2}/c\\.(?<x>" + Constants.base36 + "+)\\.(?<y>" + Constants.base36 + "+)\\.dat$", Pattern.CASE_INSENSITIVE), null, 36) {
		@Override
		public ChunkStorage createChunkStorage(final Path root) {
			return new AlphaChunkStorage(LevelCompressor.GZIP, root);
		}
	},
	;

	public static final List<FileResolvers> resolvers = List.of(FileResolvers.values());

	public final Type type;
	public final RegionHandler handler;
	public final String regionExt, chunkExt;
	public final Pattern regionPat, chunkPat;
	public final int regionRadix, chunkRadix;
	public final FileResolver regionRes, chunkRes;

	FileResolvers(Type type, RegionHandler handler, String regionExt, String chunkExt, Pattern regionPat, Pattern chunkPat, FileResolver regionRes, FileResolver chunkRes, int regionRadix, int chunkRadix) {
		this.type = type;
		this.handler = handler;
		this.regionExt = regionExt;
		this.chunkExt = chunkExt;
		this.regionPat = regionPat;
		this.chunkPat = chunkPat;
		this.regionRes = regionRes;
		this.chunkRes = chunkRes;
		this.regionRadix = regionRadix;
		this.chunkRadix = chunkRadix;
	}

	FileResolvers(Type type, RegionHandler handler, String regionExt, String chunkExt, Pattern regionPat, Pattern chunkPat) {
		this(type, handler, regionExt, chunkExt, regionPat, chunkPat, simpleResolver("r", regionExt, regionPat), simpleResolver("c", chunkExt, chunkPat), 10, 10);
	}

	FileResolvers(Type type, RegionHandler handler, String regionExt, String chunkExt) {
		this(type, handler, regionExt, chunkExt, simplePattern("r", regionExt), simplePattern("c", chunkExt));
	}

	FileResolvers(Chunk ignoredChunk, String chunkExt, Pattern chunkPat, FileResolver chunkRes, int chunkRadix) {
		this(Type.CHUNK, null, null, chunkExt, null, chunkPat, null, chunkRes, 0, chunkRadix);
	}

	public boolean matchesChunk(Path path) {
		if (chunkPat == null) {
			return false;
		}
		return chunkPat.matcher(path.toString()).find();
	}

	public Pos2i getChunkCoordinate(Path path) {
		if (chunkPat == null) {
			return null;
		}

		final var str = path.toString();
		final var matcher = chunkPat.matcher(str);

		if (!matcher.find()) {
			return null;
		}

		var map = matcher.namedGroups();
		int x = map.get("x");
		int y = map.get("y");

		int cx = Integer.parseInt(str, matcher.start(x), matcher.end(x), chunkRadix);
		int cy = Integer.parseInt(str, matcher.start(y), matcher.end(y), chunkRadix);

		return new Pos2i(cx, cy);
	}

	public boolean matchesRegion(Path path) {
		if (regionPat == null) {
			return false;
		}
		return regionPat.matcher(path.toString()).find();
	}

	public Pos2i getRegionCoordinate(Path path) {
		if (regionPat == null) {
			return null;
		}

		final var str = path.toString();
		final var matcher = regionPat.matcher(str);

		if (!matcher.find()) {
			return null;
		}

		var map = matcher.namedGroups();
		int x = map.get("x");
		int y = map.get("y");

		int cx = Integer.parseInt(str, matcher.start(x), matcher.end(x), regionRadix);
		int cy = Integer.parseInt(str, matcher.start(y), matcher.end(y), regionRadix);

		return new Pos2i(cx, cy);
	}

	public ChunkStorage createChunkStorage(Path root) {
		return new RegionedChunkStorage(handler, root, regionRes, chunkRes);
	}

	private static Pattern simplePattern(final String prefix, String extension) {
		if (extension == null) {
			return null;
		}
		return Pattern.compile(prefix + "\\.(?<x>-?\\d+)\\.(?<y>-?\\d+)\\" + extension + '$');
	}

	private static McRegionResolver simpleResolver(final String prefix, String extension, Pattern pattern) {
		if (extension == null && pattern == null) {
			return null;
		}
		Objects.requireNonNull(extension, "extension");
		Objects.requireNonNull(pattern, "pattern");
		return new McRegionResolver(prefix, extension, pattern);
	}

	private enum Type {
		REGION, CHUNK
	}

	private enum Chunk {inst}

	private enum Region {inst}
}
