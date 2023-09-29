package gay.ampflower.bundler.utils;

import gay.ampflower.bundler.world.McRegionHandler;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

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

		while((read = stream.read(buf, off + count, len - count)) >= 0) {
			count += read;
			if(known + count > minLen) {
				break;
			}
		}

		return count;
	}

	public static void writeSectors(OutputStream stream, @Nonnull byte[] read, int roff, @Nonnull byte[] buf, int woff, int len) throws IOException {
		int written = 0;

		if(buf.length != Integer.highestOneBit(buf.length)) {
			throw new IllegalArgumentException("Not a power of 2 buffer size: " + buf.length);
		}

		if(woff + len >= buf.length) {
			written = buf.length - woff;
			System.arraycopy(read, roff, buf, woff, written);
			stream.write(buf);
		}

		if(written < len) {
			int to = (len - written) & (-1 << (31 - Integer.numberOfLeadingZeros(buf.length)));
			stream.write(read, roff + written, to);
			written += to;
		}

		if(written < len) {
			int end = len - written;
			System.arraycopy(read, roff + written, buf, 0, end);
			Arrays.fill(buf, end, buf.length, (byte) 0);

			stream.write(buf);
			written += end;
		}

		final int sec = McRegionHandler.sectors(written);
		s += sec;
		logger.info("Written {} (~{} sectors), {} total", written, sec, s);
	}

	public static int[] readBigEndian(InputStream stream, byte[] buf) throws IOException {
		if((buf.length & 3) != 0) throw new IllegalArgumentException();
		if(stream.readNBytes(buf, 0, buf.length) != buf.length) throw new IOException("Incomplete read");

		final var output = new int[buf.length >> 2];
		ArrayUtils.copyBigEndianInts(buf, output);
		return output;
	}

	// A very dumb check; it doesn't try to parse it.
	public static void verifyNbt(byte[] nbt, int chunk) {
		if (nbt[0] != 0x0A) {
			throw new AssertionError("Invalid start of NBT @ " + chunk + ": " + nbt[0]);
		}
		if (nbt[nbt.length - 1] != 0x00) {
			throw new AssertionError("Invalid end of NBT @ " + chunk + ": " + nbt[nbt.length - 1]);
		}
		if ((nbt[1] | nbt[2]) != 0) {
			logger.warn("Possible corruption: Non-zero name length at chunk {}", chunk);
		}
	}
}
