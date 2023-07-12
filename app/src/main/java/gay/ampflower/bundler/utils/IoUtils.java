package gay.ampflower.bundler.utils;

import gay.ampflower.bundler.world.McRegionHandler;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class IoUtils {
	private static final Logger logger = LogUtils.logger();
	private static int s = 0;

	public static final int SHORT_STRIDE = 2, INT_STRIDE = 4, LONG_STRIDE = 8;

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

		final int sec = McRegionHandler.INSTANCE.sectors(written);
		s += sec;
		logger.info("Written {} (~{} sectors), {} total", written, sec, s);
	}

	public static void copy(byte[] read, int roff, int[] write, int woff, int len, ByteOrder order) {
		final var handle = MethodHandles.byteArrayViewVarHandle(int[].class, order);

		for(int i = 0; i < len; i++) {
			write[woff + i] = (int)handle.get(read, roff + i * INT_STRIDE);
		}
	}

	// A very dumb check; it doesn't try to parse it.
	public static void verifyNbt(byte[] nbt) {
		if(nbt[0] != 0x0A) {
			throw new AssertionError("Invalid start of NBT: " + nbt[0]);
		}
		if(nbt[nbt.length - 1] != 0x00) {
			throw new AssertionError("Invalid end of NBT: " + nbt[nbt.length - 1]);
		}
		if((nbt[1] | nbt[2]) != 0) {
			logger.warn("Possible corruption: Non-zero name length");
		}
	}
}
