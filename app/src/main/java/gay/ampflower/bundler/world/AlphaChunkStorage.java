package gay.ampflower.bundler.world;

import gay.ampflower.bundler.utils.LevelCompressor;
import gay.ampflower.bundler.utils.function.IntBiFunction;
import gay.ampflower.bundler.world.io.ChunkStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class AlphaChunkStorage implements ChunkStorage {
	private static final int ALPHA_DISK_RADIX = 36;
	private static final int ALPHA_REGION_BOUND = 64;
	private static final int ALPHA_BIT_MASK = 63;

	private static final OpenOption[] writeOptions = {StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING};

	private final LevelCompressor levelCompressor;
	private final Path workingDirectory;
	private final IntBiFunction<String> chunkResolver;

	public AlphaChunkStorage(final LevelCompressor levelCompressor, final Path workingDirectory) {
		this.levelCompressor = levelCompressor;
		this.workingDirectory = workingDirectory;
		this.chunkResolver = (x, y) -> base36(x & ALPHA_BIT_MASK) + '/' + base36(y & ALPHA_BIT_MASK) + "/c." + base36(x) + "." + base36(y) + ".dat";
	}

	@Override
	public byte[] readChunk(final int x, final int y) throws IOException {
		final var path = workingDirectory.resolve(chunkResolver.apply(x, y));

		if (Files.exists(path)) {
			return LevelCompressor.tryDecompress(Files.readAllBytes(path));
		}
		return null;
	}

	@Override
	public void writeChunk(final int x, final int y, final byte[] chunk) throws IOException {
		final var path = workingDirectory.resolve(chunkResolver.apply(x, y));

		final var parent = path.getParent();

		if (!Files.exists(parent)) {
			Files.createDirectories(parent);
		}

		Files.write(path, levelCompressor.deflate(chunk), writeOptions);
	}

	@Override
	public Region readRegion(final int x, final int y) throws IOException {
		int cx = x << Region.BIT_SHIFT;
		int cy = y << Region.BIT_SHIFT;

		byte[][] chunks = new byte[Region.CHUNK_COUNT][];

		for (int dx = 0; dx < Region.REGION_BOUND; dx++) {
			for (int dy = 0; dy < Region.REGION_BOUND; dy++) {
				// TODO: Timestamps
				chunks[dy * Region.REGION_BOUND + dx] = readChunk(cx + dx, cy + dy);
			}
		}

		return new Region(new int[Region.CHUNK_COUNT], chunks);
	}

	@Override
	public void writeRegion(final int x, final int y, final Region region) throws IOException {
		int cx = x << Region.BIT_SHIFT;
		int cy = y << Region.BIT_SHIFT;

		byte[][] chunks = region.chunks();

		for (int dx = 0; dx < Region.REGION_BOUND; dx++) {
			for (int dy = 0; dy < Region.REGION_BOUND; dy++) {
				// TODO: Timestamps
				writeChunk(cx + dx, cy + dy, chunks[dy * Region.REGION_BOUND + dx]);
			}
		}
	}

	@Override
	public boolean isNativelyRegioned() {
		return false;
	}

	private static String base36(int i) {
		return Integer.toString(i, ALPHA_DISK_RADIX);
	}
}