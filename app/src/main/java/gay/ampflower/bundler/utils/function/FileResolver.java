package gay.ampflower.bundler.utils.function;

import it.unimi.dsi.fastutil.longs.LongSet;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Ampflower
 * @since ${version}
 **/
public interface FileResolver {
	String fileName(int x, int y);

	boolean match(String name);

	default boolean match(Path path) {
		return match(path.getFileName().toString());
	}

	LongSet iterate(Path path) throws IOException;
}
