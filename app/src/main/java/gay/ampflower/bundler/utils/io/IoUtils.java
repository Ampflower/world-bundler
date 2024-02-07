package gay.ampflower.bundler.utils.io;

import gay.ampflower.bundler.nbt.NbtCompound;
import gay.ampflower.bundler.nbt.io.NbtReader;
import gay.ampflower.bundler.nbt.io.SaxNbtReader;
import gay.ampflower.bundler.nbt.io.SaxTreeWriter;
import gay.ampflower.bundler.utils.ArrayUtils;
import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.world.region.McRegionHandler;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

	public static int readIntBigEndian(InputStream stream, byte[] buf, int off) throws IOException {
		readExact(stream, buf, off, 4);
		return (int) ArrayUtils.INTS_BIG_ENDIAN.get(buf, off);
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

	public static void writeSectors(OutputStream stream, @Nonnull byte[] read, int roff, @Nonnull byte[] buf, int woff, int len) throws IOException {
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
		if((buf.length & 3) != 0) throw new IllegalArgumentException();
		if(stream.readNBytes(buf, 0, buf.length) != buf.length) throw new IOException("Incomplete read");

		final var output = new int[buf.length >> 2];
		ArrayUtils.copyBigEndianInts(buf, output);
		return output;
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
				chunk, length, ArrayUtils.urlEncoded(nbt, 3, length, 64));
		}

		final var stw = new SaxTreeWriter();
		final var bai = new ByteArrayInputStream(nbt);
		final var nr = new NbtReader(bai);

		try {
			SaxNbtReader.parse(stw, nr);
		} catch (OutOfMemoryError oome) {
			logger.warn("Exhausted available memory reading chunk @ {}", chunk, oome);
			throw new AssertionError(oome);
		} catch (IOException ioe) {
			throw new AssertionError(ioe);
		}

		logger.trace("Got {} -> {}", stw.getRootName(), stw.getRoot());

		return (NbtCompound) stw.getRoot();
	}
}
