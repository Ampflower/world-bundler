package gay.ampflower.bundler.utils.io;

import org.jetbrains.annotations.CheckReturnValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class PathUtils {
	public static Path createParents(final Path path) throws IOException {
		return Files.createDirectories(path.getParent());
	}

	@CheckReturnValue
	public static boolean isReadableRegular(final Path path, final LinkOption... options) {
		return Files.isRegularFile(path, options) && Files.isReadable(path);
	}

	@CheckReturnValue
	public static boolean isReadableDirectory(final Path path, final LinkOption... options) {
		return Files.isDirectory(path, options) && Files.isReadable(path);
	}

	public static Path findExistence(final Path... paths) {
		for (final Path path : paths) {
			if (Files.isReadable(path)) {
				return path;
			}
		}
		return null;
	}

	public static Path normalized(final String path) {
		return Path.of(path).toAbsolutePath().normalize();
	}
}
