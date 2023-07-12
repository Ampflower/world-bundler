package gay.ampflower.bundler.nbt;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class SaxNbtReader implements AutoCloseable {
	private final NbtReader reader;

	public SaxNbtReader(NbtReader reader) {
		this.reader = reader;
	}

	public void parse(SaxNbtParser parser) {

	}

	@Override
	public void close() throws Exception {
		// this.reader.close();
	}
}
