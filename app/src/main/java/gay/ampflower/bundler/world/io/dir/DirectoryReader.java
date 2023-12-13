package gay.ampflower.bundler.world.io.dir;

import gay.ampflower.bundler.utils.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;

/**
 * @author Ampflower
 * @since ${version}
 **/
public
final class DirectoryReader {
	private static final Logger logger = LogUtils.logger();

	public static DirectoryMeta run(Path regionIn) throws IOException {
		final var meta = new DirectoryMeta();

		final var list = new ArrayList<DirectoryData>();
		final var lifo = new ArrayDeque<DirectoryData>();

		Files.walkFileTree(regionIn, new FileVisitor<>() {
			@Override
			public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
				logger.debug("Visiting dir {} with attributes {}", dir, attrs);
				final var last = lifo.peek();
				if (last != null && !last.dir.equals(dir.getParent())) {
					logger.error("{}", lifo);
					throw new AssertionError(last + " isn't parent of " + dir + "; missing pop???");
				}
				lifo.push(new DirectoryData(dir));
				meta.dirs++;
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
				//logger.debug("Visiting file {} with attributes {}", file, attrs);
				final var potato = lifo.peek();
				if (potato == null) {
					throw new IllegalStateException("in: " + regionIn + ", at: " + file);
				}
				meta.files++;
				meta.size += attrs.size();
				potato.push(file, attrs);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
				logger.warn("Failed to visit {}", file, exc);
				meta.error++;
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
				if (exc != null) {
					logger.warn("Walking {} finished with error.", dir, exc);
				}
				final var last = lifo.poll();
				if (last == null) {
					throw new AssertionError("Popped null data for " + dir);
				}
				if (!last.dir.equals(dir)) {
					throw new AssertionError(last + " popped for " + dir);
				}
				list.add(last);
				return FileVisitResult.CONTINUE;
			}
		});

		if (!lifo.isEmpty()) {
			throw new AssertionError("Data present in lifo: " + lifo);
		}

		logger.info("Walking complete, {}", meta);
		for (var dir : list) {
			logger.info("{} -> {}", dir.paths, dir);
		}

		meta.list = list;
		return meta;
	}
}
