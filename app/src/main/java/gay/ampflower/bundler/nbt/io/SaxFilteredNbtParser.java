package gay.ampflower.bundler.nbt.io;

import gay.ampflower.bundler.nbt.NbtType;

import java.io.IOException;

/**
 * @author Ampflower
 **/
public abstract class SaxFilteredNbtParser implements SaxNbtParser {
	protected final SaxNbtParser filtered;

	protected SaxFilteredNbtParser(final SaxNbtParser filtered) {
		this.filtered = filtered;
	}

	@Override
	public void field(final String name) throws IOException {
		this.filtered.field(name);
	}

	@Override
	public void startList(final NbtType type, final int size) throws IOException {
		this.filtered.startList(type, size);
	}

	@Override
	public void startCompound() throws IOException {
		this.filtered.startCompound();
	}

	@Override
	public void endTag() throws IOException {
		this.filtered.endTag();
	}

	@Override
	public void ofNull() throws IOException {
		this.filtered.ofNull();
	}

	@Override
	public void ofByte(final byte value) throws IOException {
		this.filtered.ofByte(value);
	}

	@Override
	public void ofShort(final short value) throws IOException {
		this.filtered.ofShort(value);
	}

	@Override
	public void ofInt(final int value) throws IOException {
		this.filtered.ofInt(value);
	}

	@Override
	public void ofLong(final long value) throws IOException {
		this.filtered.ofLong(value);
	}

	@Override
	public void ofFloat(final float value) throws IOException {
		this.filtered.ofFloat(value);
	}

	@Override
	public void ofDouble(final double value) throws IOException {
		this.filtered.ofDouble(value);
	}

	@Override
	public void ofString(final String value) throws IOException {
		this.filtered.ofString(value);
	}

	@Override
	public void ofByteArray(final byte[] value) throws IOException {
		this.filtered.ofByteArray(value);
	}

	@Override
	public void ofIntArray(final int[] value) throws IOException {
		this.filtered.ofIntArray(value);
	}

	@Override
	public void ofLongArray(final long[] value) throws IOException {
		this.filtered.ofLongArray(value);
	}
}
