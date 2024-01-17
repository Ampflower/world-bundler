package gay.ampflower.bundler.recovery;

import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.world.Region;
import gay.ampflower.bundler.world.RegionedChunkStorage;
import gay.ampflower.bundler.world.io.ChunkStorage;
import gay.ampflower.bundler.world.io.resolvers.FileResolvers;
import gay.ampflower.bundler.world.region.McRegionRecoveryHandler;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class Recovery {
	private static final Logger logger = LogUtils.logger();

	public static void main(String[] args) throws IOException {
		final Path inputPath = Path.of(args[0]);
		final Path outputPath = Path.of(args[1]);

		final var recovery = FileResolvers.AnvilRecovery.createChunkStorage(inputPath);
		new RegionedChunkStorage(McRegionRecoveryHandler.INSTANCE, inputPath,
			FileResolvers.McRegion.regionRes, FileResolvers.McRegion.chunkRes);

		final var output = FileResolvers.Anvil.createChunkStorage(outputPath);

		final var itr = recovery.iterateRegions();
		logger.info("{} -> {}", inputPath, itr);
		while (itr.hasNext()) {
			recover(output, itr.next());
		}
	}

	private static void recover(ChunkStorage output, Region recoveredRegion) throws IOException {
		logger.trace("Raw: {}", recoveredRegion);
		if (recoveredRegion == null || recoveredRegion.isEmpty()) {
			logger.warn("Failed to read a region");
			return;
		}
		logger.info("Got [{},{}]: {}", recoveredRegion.x(), recoveredRegion.y(), recoveredRegion.popCount());

		final int x = recoveredRegion.x(), y = recoveredRegion.y();

		final var swap = output.readRegion(x, y);

		if (swap == null) {
			logger.info("Putting region @ recovery[{},{}] in swap as is", x, y);
			output.writeRegion(x, y, recoveredRegion);
			return;
		}

		final var recoveredChunks = recoveredRegion.chunks();
		final var swapChunks = swap.chunks();
		for (int i = 0; i < Region.CHUNK_COUNT; i++) {
			final var recoveredChunk = recoveredChunks[i];
			if (recoveredChunk == null || recoveredChunk.size() == 0) continue;

			final var swapChunk = swapChunks[i];
			if (swapChunk == null || swapChunk.size() == 0) {
				logger.info("Putting chunk @ recovery[{},{}][{}] in swap as is", x, y, i);
				swapChunks[i] = recoveredChunks[i];
				continue;
			}

			if (!Arrays.equals(recoveredChunk.array(), swapChunk.array())) {
				logger.warn("Chunk @ region[{},{}][{}] mismatch, doing invasive comparison", x, y, i);

				if (recoveredChunk.timestamp() > swapChunk.timestamp()) {
					logger.info("Recovery is newer than swap ({} > {}), assuming okay", recoveredChunk.timestamp(), swapChunk.timestamp());
					swapChunks[i] = recoveredChunk;
				}
			}
		}

		output.writeRegion(x, y, swap);
	}
}
