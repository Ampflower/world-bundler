package gay.ampflower.bundler.utils.io;

import gay.ampflower.bundler.utils.Mint;
import gay.ampflower.bundler.utils.SizeUtils;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.InvalidMarkException;

/**
 * @author Ampflower
 **/
public class MemoryInputStream extends InputStream {
	protected final MemorySegment channel;
	private final long limit;
	private long index, mark, markLimit;
	private volatile boolean closed;

	private MemoryInputStream(final MemorySegment channel, final long index, final long limit) throws IOException {
		final long size = channel.byteSize();

		if (limit > size) {
			throw new IndexOutOfBoundsException("limit (" + limit + ") exceeds size (" + size + ")");
		}

		if (index > limit) {
			throw new IndexOutOfBoundsException("index (" + index + ") exceeds limit (" + limit + ")");
		}

		this.channel = channel;
		this.index = index;
		this.limit = limit;
	}

	public static MemoryInputStream ofStartEnd(
		final MemorySegment channel,
		final long start,
		final long end
	) throws IOException {
		return new MemoryInputStream(channel, start, end);
	}

	public static MemoryInputStream ofOffsetLength(
		final MemorySegment channel,
		final long offset,
		final long length
	) throws IOException {
		final long size = channel.byteSize();
		final long limit = offset + length;

		if (limit > size) {
			throw new IndexOutOfBoundsException("offset (" + offset + ") + length (" + length + ") exceeds size (" + size + ")");
		}

		return new MemoryInputStream(channel, offset, limit);
	}

	public static MemoryInputStream ofOffset(final MemorySegment channel, final long position) throws IOException {
		return new MemoryInputStream(channel, position, channel.byteSize());
	}

	public static MemoryInputStream ofFull(final MemorySegment channel) throws IOException {
		return new MemoryInputStream(channel, 0, channel.byteSize());
	}

	@Override
	public int read(@NotNull final byte[] bytes, final int off, int len) throws IOException {
		assertOpen();
		if (this.index + len > this.limit) {
			len = this.available();
		}
		this.copyTo(bytes, off, len);
		return len;
	}

	@Override
	public byte[] readNBytes(int len) throws IOException {
		assertOpen();
		if (this.index + len > this.limit) {
			len = this.available();
		}
		return alloc(len);
	}

	@Override
	public int readNBytes(final byte[] bytes, final int off, final int len) throws IOException {
		// just inline.
		return read(bytes, off, len);
	}

	@Override
	public byte[] readAllBytes() throws IOException {
		assertOpen();
		final long len = availableLong();
		if (len < 0 || len > Integer.MAX_VALUE) {
			throw new OutOfMemoryError("array cannot be allocated, requires: " + SizeUtils.displaySize(len));
		}
		return alloc((int) len);
	}

	private byte[] alloc(final int length) {
		final byte[] bytes = new byte[length];
		this.copyTo(bytes, 0, length);
		return bytes;
	}

	private void copyTo(final byte[] bytes, final int offset, final int length) {
		MemorySegment.copy(this.channel, ValueLayout.JAVA_BYTE, this.index, bytes, offset, length);
		this.index += length;
	}

	@Override
	public long skip(final long n) throws IOException {
		assertOpen();

		if (n < 0) {
			return 0;
		}

		final long index = this.index;
		this.index = Math.min(index + n, this.limit);
		return this.index - index;
	}

	@Override
	public void skipNBytes(final long n) throws IOException {
		// We directly drive the channel, it makes no difference.
		if (n > 0 && skip(n) != n) {
			throw new EOFException();
		}
	}

	/**
	 * Non-blocking availability. Will return 0 when closed.
	 *
	 * @implNote As the backing is a straight up memory-mapped file channel,
	 * 	it will report {@link Integer#MAX_VALUE} in the event the available
	 * 	byte count exceeds the signed 32-bit integer limit.
	 */
	@Override
	public int available() {
		return Mint.clampAsInt(availableLong());
	}

	public long availableLong() {
		if (this.closed) {
			return 0L;
		}
		return this.limit - this.index;
	}

	/**
	 * Note: This does *NOT* close the backing MemorySegment.
	 */
	@Override
	@MustBeInvokedByOverriders
	public void close() throws IOException {
		this.closed = true;
		this.index = this.limit;
	}

	@Override
	public void mark(final int readLimit) {
		this.mark((long) readLimit);
	}

	public void mark(final long readLimit) {
		final long index = this.index;
		this.mark = index;
		this.markLimit = Mint.addClamp(index, readLimit);
	}

	@Override
	public void reset() throws IOException {
		this.assertOpen();
		if (this.index > this.markLimit) {
			throw new InvalidMarkException();
		}
		this.index = this.mark;
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public long transferTo(final OutputStream out) throws IOException {
		return super.transferTo(out);
	}

	@Override
	public int read() throws IOException {
		this.assertOpen();
		if (this.index >= this.limit) {
			return -1;
		}
		return this.channel.get(ValueLayout.JAVA_BYTE, this.index++);
	}

	private void assertOpen() throws IOException {
		if (this.closed) {
			throw new IOException("Closed");
		}
	}
}
