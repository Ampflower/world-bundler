package gay.ampflower.bundler.utils.io;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class PathUtils {
	public static Path createParents(final Path path) throws IOException {
		return Files.createDirectories(path.getParent());
	}
}
