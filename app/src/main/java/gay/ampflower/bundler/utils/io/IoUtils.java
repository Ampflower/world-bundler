package gay.ampflower.bundler.utils.io;

import gay.ampflower.bundler.nbt.NbtCompound;
import gay.ampflower.bundler.nbt.io.NbtReader;
import gay.ampflower.bundler.nbt.io.SaxNbtReader;
import gay.ampflower.bundler.nbt.io.SaxTreeWriter;
import gay.ampflower.bundler.utils.ArrayUtils;
import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.utils.Mint;
import gay.ampflower.bundler.world.region.McRegionHandler;
import org.apache.commons.compress.utils.CountingInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.function.Consumer;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class IoUtils {
	private static final Logger logger = LogUtils.logger();
	private static int s = 0;

	public static int readMin(InputStream stream, byte[] buf, int off, int minLen) throws IOException {
		int read, count = 0, len = buf.length - off;

		while((read = stream.read(buf, off + count, len - count)) >= 0) {
			count += read;
			if(count > minLen) {
				break;
			}
		}

		return count;
	}

	public static int readMin(InputStream stream, byte[] buf, int off, int minLen, int known) throws IOException {
		int read, count = 0, len = buf.length - off;

		while ((read = stream.read(buf, off + count, len - count)) >= 0) {
			count += read;
			if (known + count > minLen) {
				break;
			}
		}

		return count;
	}

	public static void readExact(InputStream stream, byte[] buf, int off, int len) throws IOException {
		int read;
		if ((read = stream.read(buf, off, len)) != len) {
			throw new IOException("Expected " + len + ", got " + read);
		}
	}

	public static void writeShortsBigEndian(OutputStream stream, byte[] buf, short[] shorts) throws IOException {
		if (buf.length % ArrayUtils.SHORT_STRIDE != 0) {
			throw new IllegalArgumentException("buf & 3 != 0: " + buf.length % ArrayUtils.SHORT_STRIDE);
		}
		final int len = buf.length / ArrayUtils.SHORT_STRIDE, fast = shorts.length / len;
		int off = 0;
		for (int count = 0; count < fast; count++, off += len) {
			copyAndWrite(stream, shorts, off, buf, len, ByteOrder.BIG_ENDIAN);
		}
		copyAndWrite(stream, shorts, off, buf, shorts.length % len, ByteOrder.BIG_ENDIAN);
	}

	private static void copyAndWrite(OutputStream stream, short[] ints, int off, byte[] buf, int len, ByteOrder order) throws IOException {
		ArrayUtils.copy(ints, off, buf, 0, len, order);
		stream.write(buf, 0, len * ArrayUtils.SHORT_STRIDE);
	}

	public static int readIntBigEndian(InputStream stream, byte[] buf, int off) throws IOException {
		readExact(stream, buf, off, 4);
		return (int) ArrayUtils.INTS_BIG_ENDIAN.get(buf, off);
	}

	public static void writeIntsBigEndian(OutputStream stream, byte[] buf, int[] ints) throws IOException {
		if (buf.length % ArrayUtils.INT_STRIDE != 0) {
			throw new IllegalArgumentException("buf & 3 != 0: " + buf.length % ArrayUtils.INT_STRIDE);
		}
		final int len = buf.length / ArrayUtils.INT_STRIDE, fast = ints.length / len;
		int off = 0;
		for (int count = 0; count < fast; count++, off += len) {
			copyAndWrite(stream, ints, off, buf, len, ByteOrder.BIG_ENDIAN);
		}
		copyAndWrite(stream, ints, off, buf, ints.length % len, ByteOrder.BIG_ENDIAN);
	}

	private static void copyAndWrite(OutputStream stream, int[] ints, int off, byte[] buf, int len, ByteOrder order) throws IOException {
		ArrayUtils.copy(ints, off, buf, 0, len, order);
		stream.write(buf, 0, len * ArrayUtils.INT_STRIDE);
	}

	public static void writeLongsBigEndian(OutputStream stream, byte[] buf, long[] longs) throws IOException {
		if (buf.length % ArrayUtils.LONG_STRIDE != 0) {
			throw new IllegalArgumentException("buf & 3 != 0: " + buf.length % ArrayUtils.LONG_STRIDE);
		}
		final int len = buf.length / ArrayUtils.LONG_STRIDE, fast = longs.length / len;
		int off = 0;
		for (int count = 0; count < fast; count++, off += len) {
			copyAndWrite(stream, longs, off, buf, len, ByteOrder.BIG_ENDIAN);
		}
		copyAndWrite(stream, longs, off, buf, longs.length % len, ByteOrder.BIG_ENDIAN);
	}

	private static void copyAndWrite(OutputStream stream, long[] longs, int off, byte[] buf, int len, ByteOrder order) throws IOException {
		ArrayUtils.copy(longs, off, buf, 0, len, order);
		stream.write(buf, 0, len * ArrayUtils.LONG_STRIDE);
	}


	public static void writeFloatsBigEndian(OutputStream stream, byte[] buf, float[] floats) throws IOException {
		if (buf.length % ArrayUtils.FLOAT_STRIDE != 0) {
			throw new IllegalArgumentException("buf & 3 != 0: " + buf.length % ArrayUtils.FLOAT_STRIDE);
		}
		final int len = buf.length / ArrayUtils.FLOAT_STRIDE, fast = floats.length / len;
		int off = 0;
		for (int count = 0; count < fast; count++, off += len) {
			copyAndWrite(stream, floats, off, buf, len, ByteOrder.BIG_ENDIAN);
		}
		copyAndWrite(stream, floats, off, buf, floats.length % len, ByteOrder.BIG_ENDIAN);
	}

	private static void copyAndWrite(OutputStream stream, float[] floats, int off, byte[] buf, int len, ByteOrder order) throws IOException {
		ArrayUtils.copy(floats, off, buf, 0, len, order);
		stream.write(buf, 0, len * ArrayUtils.FLOAT_STRIDE);
	}

	public static void writeDoublesBigEndian(OutputStream stream, byte[] buf, double[] doubles) throws IOException {
		if (buf.length % ArrayUtils.DOUBLE_STRIDE != 0) {
			throw new IllegalArgumentException("buf & 3 != 0: " + buf.length % ArrayUtils.DOUBLE_STRIDE);
		}
		final int len = buf.length / ArrayUtils.DOUBLE_STRIDE, fast = doubles.length / len;
		int off = 0;
		for (int count = 0; count < fast; count++, off += len) {
			copyAndWrite(stream, doubles, off, buf, len, ByteOrder.BIG_ENDIAN);
		}
		copyAndWrite(stream, doubles, off, buf, doubles.length % len, ByteOrder.BIG_ENDIAN);
	}

	private static void copyAndWrite(OutputStream stream, double[] doubles, int off, byte[] buf, int len, ByteOrder order) throws IOException {
		ArrayUtils.copy(doubles, off, buf, 0, len, order);
		stream.write(buf, 0, len * ArrayUtils.DOUBLE_STRIDE);
	}

	public static Thread asyncPipe(InputStream inputStream, OutputStream outputStream, Consumer<IOException> handler, Runnable closed) {
		return Thread.startVirtualThread(pipe(inputStream, outputStream, handler, closed));
	}

	public static Runnable pipe(InputStream inputStream, OutputStream outputStream, Consumer<IOException> handler, Runnable closed) {
		return () -> {
			try (inputStream; outputStream) {
				inputStream.transferTo(outputStream);
			} catch (IOException ioe) {
				handler.accept(ioe);
			}
			closed.run();
		};
	}

	public static void writeSectors(
		OutputStream stream,
		@NotNull byte[] read,
		int roff,
		@NotNull byte[] buf,
		int woff,
		int len
	) throws IOException {
		int written = 0;

		if (buf.length != Integer.highestOneBit(buf.length)) {
			throw new IllegalArgumentException("Not a power of 2 buffer size: " + buf.length);
		}

		if (woff + len >= buf.length) {
			written = buf.length - woff;
			System.arraycopy(read, roff, buf, woff, written);
			stream.write(buf);
			woff = 0;
		}

		if(written < len) {
			int to = (len - written) & (-1 << (31 - Integer.numberOfLeadingZeros(buf.length)));
			stream.write(read, roff + written, to);
			written += to;
		}

		if(written < len) {
			int end = len - written;
			System.arraycopy(read, roff + written, buf, woff, end);
			Arrays.fill(buf, woff + end, buf.length, (byte) 0);

			stream.write(buf);
			written += end;
		}

		final int sec = McRegionHandler.sectors(written);
		s += sec;
		logger.trace("Written {} (~{} sectors), {} total", written, sec, s);
	}

	public static int[] readBigEndian(InputStream stream, byte[] buf) throws IOException {
		if ((buf.length & 3) != 0) {
			throw new IllegalArgumentException();
		}
		final int read = stream.readNBytes(buf, 0, buf.length);
		if (read != buf.length) {
			throw new IOException("Incomplete read: expected " + buf.length + " bytes, got " + read + " bytes");
		}

		final var output = new int[buf.length >> 2];
		ArrayUtils.copyBigEndianInts(buf, output);
		return output;
	}

	public static boolean markSupported(final InputStream stream, int length) {
		if (!stream.markSupported()) {
			// We cannot return a buffer.
			return false;
		}

		// Non-compliance test.
		// In theory, a stream that fails this would have not advanced.
		try {
			stream.mark(length);
			stream.reset();
		} catch (IOException ignored) {
			return false;
		}

		return true;
	}

	public static byte @Nullable [] markRead(final InputStream stream, int length) throws IOException {
		if (!markSupported(stream, length)) {
			return null;
		}

		stream.mark(length);
		final var array = stream.readNBytes(length);
		// At this point, the stream has been altered.
		stream.reset();

		return array;
	}

	public static int markRead(
		final InputStream stream,
		final byte[] buf,
		final int offset,
		final int length
	) throws IOException {
		if (!markSupported(stream, length)) {
			return 0;
		}

		stream.mark(length);
		final int read = stream.readNBytes(buf, offset, length);
		// At this point, the stream has been altered.
		stream.reset();

		return read;
	}

	public static byte[] markRead(final PushbackInputStream stream, int length) throws IOException {
		final byte[] buf = stream.readNBytes(length);
		stream.unread(buf);
		return buf;
	}

	public static int markRead(
		final PushbackInputStream stream,
		final byte[] buf,
		final int offset,
		final int length
	) throws IOException {
		final int read = stream.readNBytes(buf, offset, length);
		stream.unread(buf, offset, read);
		return read;
	}

	public static int adjustSize(
		final SeekableByteChannel channel,
		final long offset,
		final int length
	) throws IOException {
		if (!channel.isOpen()) {
			return 0;
		}

		final long size = channel.size();

		final long available = size - offset;

		if (available <= 0) {
			return 0;
		}

		if (available < (long) length) {
			return (int) available;
		}

		return length;
	}

	public static byte[] sizedBuffer(
		final SeekableByteChannel channel,
		final long offset,
		final int length
	) throws IOException {
		return ArrayUtils.sizedBytes(adjustSize(channel, offset, length));
	}

	public static byte[] markRead(final SeekableByteChannel channel, final int length) throws IOException {
		return markRead(channel, channel.position(), length);
	}

	public static byte[] markRead(final SeekableByteChannel channel, final long offset, int length) throws IOException {
		final byte[] buf = sizedBuffer(channel, offset, length);

		if (buf.length == 0) {
			return buf;
		}

		final long previous = channel.position();
		channel.position(offset);

		final int read = readN(channel, ByteBuffer.wrap(buf));

		channel.position(previous);

		return read != buf.length ? Arrays.copyOf(buf, read) : buf;
	}

	public static int readN(final ReadableByteChannel channel, final ByteBuffer buf) throws IOException {
		int total = 0;
		int read;
		while (buf.remaining() > 0 && (read = channel.read(buf)) > 0) {
			total += read;
		}
		return total;
	}

	public static byte[] markRead(final FileChannel channel, int length) throws IOException {
		return markRead(channel, channel.position(), length);
	}

	public static byte[] markRead(final FileChannel channel, final long offset, int length) throws IOException {
		final byte[] buf = sizedBuffer(channel, offset, length);

		if (buf.length == 0) {
			return buf;
		}

		final int read = readN(channel, ByteBuffer.wrap(buf), offset);
		return read != buf.length ? Arrays.copyOf(buf, read) : buf;
	}

	public static int readN(final FileChannel channel, final ByteBuffer buf, long offset) throws IOException {
		int total = 0;
		int read;
		while (buf.remaining() > 0 && (read = channel.read(buf, offset)) > 0) {
			offset += read;
			total += read;
		}
		return total;
	}

	public static byte[] markRead(final MemorySegment segment, final long offset, int length) {
		final byte[] buf = ArrayUtils.sizedBytes(Math.min(length, Mint.clampAsInt(segment.byteSize() - offset)));

		if (buf.length == 0) {
			return buf;
		}

		MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, offset, buf, 0, buf.length);

		return buf;
	}

	public static NbtCompound verifyNbt(byte[] nbt, int chunk) {
		if (nbt[0] != 0x0A) {
			throw new AssertionError("Invalid start of NBT @ " + chunk + ": " + nbt[0]);
		}
		if (nbt[nbt.length - 1] != 0x00) {
			throw new AssertionError("Invalid end of NBT @ " + chunk + ": " + nbt[nbt.length - 1]);
		}
		final int length = ((short) ArrayUtils.SHORTS_BIG_ENDIAN.get(nbt, 1)) & 0xFFFF;
		if (length != 0) {
			logger.warn("Possible corruption: Non-zero name length at chunk {}: Got {}: {}",
				chunk, length, ArrayUtils.urlEncoded(nbt, 3, length, 64)
			);
		}

		final var stw = new SaxTreeWriter();
		final var bai = new ByteArrayInputStream(nbt);
		final var cis = new CountingInputStream(bai);
		final var nr = new NbtReader(cis);

		try {
			SaxNbtReader.parse(stw, nr);
		} catch (OutOfMemoryError oome) {
			logger.warn("Exhausted available memory reading chunk @ {}", chunk, oome);
			logger.warn("Caught at path: {}\nRead {} bytes", stw, cis.getBytesRead());
			throw new AssertionError(oome);
		} catch (IOException ioe) {
			logger.warn("Caught at path: {}\nRead {} bytes", stw, cis.getBytesRead());
			throw new AssertionError(ioe);
		}

		logger.trace("Got {} -> {}", stw.getRootName(), stw.getRoot());

		return (NbtCompound) stw.getRoot();
	}
}
