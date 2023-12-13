package gay.ampflower.bundler.utils.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class CountingOutputStream extends FilterOutputStream {
	private long transferred;

	/**
	 * Creates an output stream filter built on top of the specified
	 * underlying output stream.
	 *
	 * @param out the underlying output stream to be assigned to
	 *            the field {@code this.out} for later use, or
	 *            {@code null} if this instance is to be
	 *            created without an underlying stream.
	 */
	public CountingOutputStream(final OutputStream out) {
		super(out);
	}

	@Override
	public void write(final int b) throws IOException {
		this.out.write(b);
		transferred++;
	}

	@Override
	public void write(final byte[] b) throws IOException {
		this.out.write(b);
		transferred += b.length;
	}

	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		this.out.write(b, off, len);
		transferred += len;
	}

	public long getTransferred() {
		return this.transferred;
	}
}
