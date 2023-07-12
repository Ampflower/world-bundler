package gay.ampflower.bundler.utils;

import javax.annotation.Nonnull;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class LimitedInputStream extends FilterInputStream {
	private static final VarHandle REMAINING;

	// It may not be final.
	@SuppressWarnings("FieldMayBeFinal")
	private volatile int remaining;

	static {
		try {
			REMAINING = MethodHandles.lookup().findVarHandle(LimitedInputStream.class, "remaining", int.class);
		} catch (ReflectiveOperationException roe) {
			throw new AssertionError(roe);
		}
	}

	public LimitedInputStream(final InputStream in, final int remaining) {
		super(in);
		this.remaining = remaining;
	}

	@Override
	public int read() throws IOException {
		if(remaining == 0) {
			return -1;
		}
		check();

		int ret = super.read();
		// >= intentional; read only returns negative on error
		if(ret >= 0) {
			REMAINING.getAndAdd(this, -1);
		}
		return ret;
	}

	@Override
	public int read(@Nonnull final byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read(@Nonnull final byte[] b, final int off, final int len) throws IOException {
		if(remaining == 0) {
			return -1;
		}
		check();

		int ret = super.read(b, off, Math.min(len, remaining));
		if(ret > 0) {
			REMAINING.getAndAdd(this, -ret);
		}
		return ret;
	}

	@Override
	public int available() throws IOException {
		if(this.in == null) {
			return 0;
		}
		return Math.min(super.available(), remaining);
	}

	@Override
	public long skip(final long n) throws IOException {
		if(remaining <= 0) {
			return 0;
		}
		check();

		long ret = super.skip(Math.min(n, remaining));
		if(ret > 0L) {
			REMAINING.getAndAdd(this, -(int)ret);
		}
		return ret;
	}

	@Override
	public void close() throws IOException {
		this.in = null;
	}

	private void check() throws IOException {
		if(in == null) {
			throw new IOException("Stream closed");
		}

		if(remaining == 0) {
			throw new EOFException();
		}

		if(remaining < 0) {
			throw new AssertionError("Bugcheck: Over-read " + remaining);
		}
	}
}
