package gay.ampflower.bundler.world.io;

import java.nio.file.Path;

/**
 * @author Ampflower
 * @since ${version}
 **/
public interface WorldReader {

	void start();

	static WorldReader ofDirectory(Path root) {
		return new DirectoryReader(root);
	}

	static WorldReader ofArchive(Path root) {
		// TODO:
		throw new UnsupportedOperationException();
	}

}
