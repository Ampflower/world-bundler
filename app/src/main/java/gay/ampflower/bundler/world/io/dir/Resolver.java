package gay.ampflower.bundler.world.io.dir;

import java.io.File;
import java.nio.file.Path;

/**
 * @author Ampflower
 * @since ${version}
 **/
public interface Resolver<T> {
	T getParent(T t);

	String getFileName(T t);

	final class Paths implements Resolver<Path> {
		public static final Paths inst = new Paths();

		@Override
		public Path getParent(final Path path) {
			return path.getParent();
		}

		@Override
		public String getFileName(final Path path) {
			return path.getFileName().toString();
		}
	}

	final class Files implements Resolver<File> {
		public static final Files inst = new Files();

		@Override
		public File getParent(final File file) {
			return file.getParentFile();
		}

		@Override
		public String getFileName(final File file) {
			return file.getName();
		}
	}
}
