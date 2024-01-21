package gay.ampflower.bundler.recovery;

import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.world.RegionedChunkStorage;
import gay.ampflower.bundler.world.io.resolvers.FileResolvers;
import gay.ampflower.bundler.world.region.McRegionRecoveryHandler;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

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

		final var output = new RecoveryLevelStorage(outputPath, FileResolvers.Anvil);

		final var itr = recovery.iterateRegions();
		logger.info("{} -> {}", inputPath, itr);
		while (itr.hasNext()) {
			output.recover(itr.next());
		}
	}
}
