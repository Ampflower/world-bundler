package gay.ampflower.bundler.world.io;

import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.utils.SizeUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.LongFunction;

/**
 * @author Ampflower
 * @since ${version}
 **/
public
class DirectoryReader extends Thread implements WorldReader {
	private static final Logger logger = LogUtils.logger();

	private final Path root;

	DirectoryReader(Path root) {
		this.root = root;
	}

	public void run() {
		var map = new Long2ObjectAVLTreeMap<Set<Path>>();

		logger.info("Scanning directories...");

		try (var stream = Files.walk(root)) {
			long totalSize = 0, totalCount = 0;

			var itr = stream.iterator();
			while (itr.hasNext()) {
				var file = itr.next();

				if (!Files.isRegularFile(file)) continue;

				var size = Files.size(file);

				if (size == 0) continue;

				totalSize += size;
				totalCount++;

				map.computeIfAbsent(size, (LongFunction<Set<Path>>) (long l) -> new HashSet<>()).add(file);
			}

			logger.info("Preparing {} files with {} total.", totalCount, SizeUtils.displaySize(totalSize));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
