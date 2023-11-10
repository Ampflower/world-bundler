package gay.ampflower.bundler.world;

import gay.ampflower.bundler.utils.LevelCompressor;
import gay.ampflower.bundler.utils.function.IntBiFunction;
import gay.ampflower.bundler.world.io.ChunkReader;
import gay.ampflower.bundler.world.io.ChunkStorage;
import gay.ampflower.bundler.world.io.ChunkWriter;
import gay.ampflower.bundler.world.io.RegionHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class RegionedChunkStorage implements ChunkStorage {
	private final RegionHandler regionHandler;
	private final Path workingDirectory;
	private final IntBiFunction<String> regionResolver;
	private final IntBiFunction<String> chunkResolver;

	private static final OpenOption[] writeOptions = {StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING};

	public RegionedChunkStorage(final RegionHandler regionHandler, final Path workingDirectory) {
		this.regionHandler = regionHandler;
		this.workingDirectory = workingDirectory;
		this.regionResolver = (x, y) -> "r." + x + "." + y + ".mca";
		this.chunkResolver = (x, y) -> "c." + x + "." + y + ".mcc";
	}

	@Override
	public byte[] readChunk(final int x, final int y) throws IOException {
		// FIXME: Optimise this better
		return readRegion(x >> 5, y >> 5).getChunk(x & Region.BIT_MASK, y & Region.BIT_MASK);
	}

	@Override
	public void writeChunk(final int x, final int y, final byte[] chunk) throws IOException {
		// FIXME: Optimise this better
		Region region = readRegion(x >> Region.BIT_SHIFT, y >> Region.BIT_SHIFT);
		region.setChunk(x & Region.BIT_MASK, y & Region.BIT_MASK, chunk);
		writeRegion(x >> Region.BIT_SHIFT, y >> Region.BIT_SHIFT, region);
	}

	@Override
	public Region readRegion(int x, int y) throws IOException {
		var path = workingDirectory.resolve(regionResolver.apply(x, y));

		if (Files.exists(path)) {
			try (final var input = Files.newInputStream(path)) {
				return regionHandler.readRegion(input);
			}
		}

		return null;
	}

	@Override
	public void writeRegion(int x, int y, Region region) throws IOException {
		var path = workingDirectory.resolve(regionResolver.apply(x, y));

		if (Files.isDirectory(path)) {
			throw new IOException("Region " + x + ", " + y + " is a directory?");
		}

		try (final var output = Files.newOutputStream(path, writeOptions)) {
			regionHandler.writeRegion(output, region, new McRegionChunkReader(x, y));
		}
	}

	@Override
	public boolean isNativelyRegioned() {
		return true;
	}

	private class McRegionChunkReader implements ChunkReader, ChunkWriter {
		private final int regionX;
		private final int regionY;

		private McRegionChunkReader(final int regionX, final int regionY) {
			this.regionX = regionX;
			this.regionY = regionY;
		}

		@Override
		public byte[] readChunk(final int i, final LevelCompressor compressor) throws IOException {
			final int y = regionY * Region.REGION_BOUND + (i >> 4);
			final int x = regionX * Region.REGION_BOUND + (i & 31);

			final var path = workingDirectory.resolve(chunkResolver.apply(x, y));

			if (Files.exists(path)) {
				try (final var input = Files.newInputStream(path);
					  final var inflater = compressor.inflater(input)) {
					return inflater.readAllBytes();
				}
			}

			return null;
		}

		@Override
		public void writeChunk(final int i, final byte[] data) throws IOException {
			final int y = regionY * Region.REGION_BOUND + (i >> 4);
			final int x = regionX * Region.REGION_BOUND + (i & 31);

			final var path = workingDirectory.resolve(chunkResolver.apply(x, y));

			if (Files.isDirectory(path)) {
				throw new IOException("Chunk " + x + ", " + y + " is a directory?");
			}

			Files.write(path, data, writeOptions);
		}
	}
}
