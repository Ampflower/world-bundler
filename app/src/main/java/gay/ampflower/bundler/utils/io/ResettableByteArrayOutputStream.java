package gay.ampflower.bundler.utils.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.zip.Checksum;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class ResettableByteArrayOutputStream extends OutputStream {
	private byte[] buf;
	private int count;

	private final int sector;

	public ResettableByteArrayOutputStream(int expectedSize, int sector) {
		super();
		if (sector <= 0 || sector != (sector & -sector)) {
			throw new IllegalArgumentException("sector is not power of two");
		}
		this.buf = new byte[expectedSize];
		this.sector = sector;
	}

	private void ensureSize(int add) {
		final int expected = count + add;
		if (buf.length < expected) {
			this.buf = Arrays.copyOf(this.buf, (expected & -sector) + sector);
		}
	}

	@Override
	public void write(final int b) throws IOException {
		ensureSize(1);
		buf[count++] = (byte) b;
	}


	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		if (len < 0 || off < 0 || b.length < off + len) {
			throw new IllegalArgumentException();
		}
		ensureSize(len);
		System.arraycopy(b, off, this.buf, this.count, len);
		this.count += len;
	}

	public byte[] getByteArray() {
		return Arrays.copyOf(buf, count);
	}

	public byte[] getByteArrayRaw() {
		return buf;
	}

	public int getCount() {
		return count;
	}

	public long checksum(Checksum checksum) {
		checksum.update(buf, 0, count);
		return checksum.getValue();
	}

	public void transferTo(OutputStream stream) throws IOException {
		stream.write(buf, 0, count);
	}

	@Override
	public void close() {
		this.count = 0;
	}
}
