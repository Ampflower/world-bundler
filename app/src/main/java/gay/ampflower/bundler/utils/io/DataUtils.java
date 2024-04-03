package gay.ampflower.bundler.utils.io;

import gay.ampflower.bundler.utils.ArrayUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import static gay.ampflower.bundler.utils.ArrayUtils.SHORT_STRIDE;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class DataUtils {
	public static final Charset CHARSET = StandardCharsets.UTF_8;
	public static final int MAX_STRING_SIZE = 0xFFFF;

	public static int readStringSize(byte[] bytes, int off) {
		return ((short) ArrayUtils.SHORTS_BIG_ENDIAN.get(bytes, off)) & MAX_STRING_SIZE;
	}

	public static String readString(byte[] bytes, int off) {
		return readString(bytes, off + SHORT_STRIDE, readStringSize(bytes, off));
	}

	public static String readString(byte[] bytes, int off, int len) {
		return new String(bytes, off, len, CHARSET);
	}

	public static String readString(InputStream stream) throws IOException {
		final var sizeBytes = stream.readNBytes(2);

		final int size = DataUtils.readStringSize(sizeBytes, 0);
		final var array = stream.readNBytes(size);

		return DataUtils.readString(array, 0, size);
	}

	// Could be better; this is kinda fast & lazy
	public static byte[] writeString(String string) {
		if (string == null || string.isEmpty()) {
			return new byte[2];
		}

		if (string.length() > MAX_STRING_SIZE) {
			throw new IllegalArgumentException("input length");
		}

		final var raw = string.getBytes(CHARSET);
		if (raw.length > MAX_STRING_SIZE) {
			throw new IllegalArgumentException("encoded length");
		}

		final var out = new byte[raw.length + 2];

		ArrayUtils.SHORTS_BIG_ENDIAN.set(out, 0, (short) raw.length);
		System.arraycopy(raw, 0, out, SHORT_STRIDE, raw.length);

		return out;
	}
}
