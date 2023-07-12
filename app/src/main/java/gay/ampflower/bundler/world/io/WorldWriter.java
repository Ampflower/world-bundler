package gay.ampflower.bundler.world.io;

import java.io.OutputStream;
import java.nio.file.Path;

/**
 * @author Ampflower
 * @since ${version}
 **/
public interface WorldWriter extends AutoCloseable {

	static WorldWriter ofDirectory(Path path) {
		return new DirectoryWriter(path);
	}

	static WorldWriter ofArchive(Path path) {
		throw new UnsupportedOperationException();
	}

	static WorldWriter ofArchive(OutputStream stream) {
		throw new UnsupportedOperationException();
	}
}
