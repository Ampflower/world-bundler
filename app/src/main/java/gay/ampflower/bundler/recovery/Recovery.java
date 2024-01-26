package gay.ampflower.bundler.recovery;

import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.utils.pos.Pos2i;
import gay.ampflower.bundler.world.Chunk;
import gay.ampflower.bundler.world.Region;
import gay.ampflower.bundler.world.RegionedChunkStorage;
import gay.ampflower.bundler.world.io.ChunkReader;
import gay.ampflower.bundler.world.io.dir.DirectoryMeta;
import gay.ampflower.bundler.world.io.dir.DirectoryReader;
import gay.ampflower.bundler.world.io.resolvers.FileResolvers;
import gay.ampflower.bundler.world.region.McRegionRecoveryHandler;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class Recovery {
	private static final Logger logger = LogUtils.logger();

	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			logger.warn("Please pass: <input> <output>");
			System.exit(1);
			return;
		}

		final Path inputPath = Path.of(args[0]);
		final Path outputPath = Path.of(args[1]);

		logger.info("Discovering {}...", inputPath);

		final DirectoryMeta meta = DirectoryReader.run(inputPath);

		logger.info("Resolved {} directories with {} files.", meta.dirs, meta.files);

		final var exec = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors());
		final var regions = new ConcurrentLinkedQueue<Region>();

		for (final var data : meta.list) {
			final var paths = data.paths;
			exec.submit(() -> {
				for (final var path : paths) {
					try (final var in = Files.newInputStream(path)) {
						regions.add(McRegionRecoveryHandler.INSTANCE.readRegion(0, 0, in));
					} catch (IOException ioe) {
						logger.warn("Unable to read {}:", path, ioe);
					}
				}
			});

			for (final var entry : data.regions.entrySet()) {
				final var resolver = entry.getKey();
				final var values = entry.getValue();

				exec.submit(() -> {
					final var storage = resolver.createChunkStorage(data.dir);
					final var itr = values.iterator();
					while (itr.hasNext()) {
						final long value = itr.nextLong();
						final int x = Pos2i.x(value), y = Pos2i.y(value);
						Region primary = null, secondary = null;

						try {
							primary = storage.readRegion(x, y);
						} catch (IOException ioe) {
							logger.warn("Unable to read {}, {}: ", x, y, ioe);
						}

						final var other = data.dir.resolve(resolver.regionRes.fileName(x, y));
						try (final var in = Files.newInputStream(other)) {
							final ChunkReader reader;
							if (storage instanceof RegionedChunkStorage rcs) {
								reader = rcs.new McRegionChunkReader(x, y);
							} else {
								reader = new ChunkReader.McLogger();
							}
							secondary = McRegionRecoveryHandler.INSTANCE.readRegion(x, y, in, reader);
						} catch (IOException ioe) {
							logger.warn("Unable to read {}, {} with Recovery:", x, y, ioe);
						}

						logger.debug("primary == {}, secondary == {}", isEmpty(primary), isEmpty(secondary));
						logger.trace("primary == {}, secondary == {}", primary, secondary);
						if (isEmpty(primary)) {
							if (!isEmpty(secondary)) {
								logger.debug("adding secondary");
								regions.add(secondary);
							}
						} else if (isEmpty(secondary)) {
							logger.debug("adding primary");
							regions.add(primary);
						} else {
							logger.debug("merging");
							final Chunk[] chunks = primary.chunks().clone();

							for (int i = 0; i < chunks.length; i++) {
								if (chunks[i] == null || chunks[i].size() == 0) {
									chunks[i] = secondary.chunks()[i];
								}
							}

							regions.add(new Region(x, y, chunks, secondary.meta()));
						}
					}
				});
			}
		}

		// Everything has been queued
		exec.shutdown();

		final var output = new RecoveryLevelStorage(outputPath, FileResolvers.Anvil);

		do {
			LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5L));
			poll(regions, output);
		} while (!exec.isTerminated());
		poll(regions, output);
	}

	private static void poll(Queue<Region> regions, RecoveryLevelStorage output) throws IOException {
		Region region;
		while ((region = regions.poll()) != null) {
			output.recover(region);
		}
	}

	private static boolean isEmpty(Region region) {
		if (region == null) {
			return true;
		}
		if (!region.isEmpty()) {
			return false;
		}
		final var meta = region.meta();
		if (meta instanceof Collection<?> collection) {
			return !collection.isEmpty();
		}
		return meta == null;
	}
}
