package gay.ampflower.bundler.world.io;

import java.nio.file.Path;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class DirectoryWriter implements WorldWriter {
	private final Path root;

	DirectoryWriter(Path root) {
		this.root = root;
	}

	@Override
	public void close() throws Exception {

	}
}
