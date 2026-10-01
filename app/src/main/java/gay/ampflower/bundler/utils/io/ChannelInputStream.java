package gay.ampflower.bundler.utils.io;

import gay.ampflower.bundler.utils.Mint;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;

/**
 * @author Ampflower
 **/
public class ChannelInputStream extends InputStream {
	protected final FileChannel channel;
	private final long limit;
	private long index, mark, markLimit;
	private final ByteBuffer oneByte = ByteBuffer.wrap(new byte[1]);
	private volatile boolean closed;

	private ChannelInputStream(final FileChannel channel, final long index, final long limit) throws IOException {
		final long size = channel.size();

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

	public static ChannelInputStream ofStartEnd(
		final FileChannel channel,
		final long start,
		final long end
	) throws IOException {
		return new ChannelInputStream(channel, start, end);
	}

	public static ChannelInputStream ofOffsetLength(
		final FileChannel channel,
		final long offset,
		final long length
	) throws IOException {
		final long size = channel.size();
		final long limit = offset + length;

		if (limit > size) {
			throw new IndexOutOfBoundsException("offset (" + offset + ") + length (" + length + ") exceeds size (" + size + ")");
		}

		return new ChannelInputStream(channel, offset, limit);
	}

	public static ChannelInputStream ofOffset(final FileChannel channel, final long position) throws IOException {
		return new ChannelInputStream(channel, position, channel.size());
	}

	public static ChannelInputStream ofPrevious(final FileChannel channel) throws IOException {
		return new ChannelInputStream(channel, 0, channel.position());
	}

	public static ChannelInputStream ofCurrent(final FileChannel channel) throws IOException {
		return new ChannelInputStream(channel, channel.position(), channel.size());
	}

	public static ChannelInputStream ofFull(final FileChannel channel) throws IOException {
		return new ChannelInputStream(channel, 0, channel.size());
	}

	@Override
	public int read(@NotNull final byte[] b, final int off, int len) throws IOException {
		if (this.index + len > this.limit) {
			len = this.available();
		}
		int read = this.channel.read(ByteBuffer.wrap(b, off, len), this.index);
		this.index += read;
		return read;
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
		if (!this.channel.isOpen()) {
			return 0L;
		}
		return this.limit - this.index;
	}

	/**
	 * Note: This does *NOT* close the backing FileChannel.
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
		this.oneByte.clear();
		final long read = this.channel.read(this.oneByte, this.index++);
		if (read != 1) {
			throw new IOException("Expected 1, got " + read);
		}
		return this.oneByte.get(0);
	}

	private void assertOpen() throws IOException {
		if (this.closed) {
			throw new IOException("Closed");
		}
		if (!this.channel.isOpen()) {
			throw new ClosedChannelException();
		}
	}
}
