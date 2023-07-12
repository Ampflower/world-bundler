package gay.ampflower.bundler.world.io;

import java.io.OutputStream;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class ArchiveWriter implements WorldWriter {
	private final OutputStream stream;

	ArchiveWriter(OutputStream stream) {
		this.stream = stream;
	}

	@Override
	public void close() throws Exception {
		stream.close();
	}
}
