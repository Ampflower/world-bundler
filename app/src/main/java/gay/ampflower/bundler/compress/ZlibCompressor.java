package gay.ampflower.bundler.compress;

import gay.ampflower.bundler.utils.ArrayUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class ZlibCompressor implements Compressor {
	public static final ZlibCompressor INSTANCE = new ZlibCompressor();

	@Override
	public OutputStream deflater(final OutputStream stream) {
		return new DeflaterOutputStream(stream);
	}

	@Override
	public InputStream inflater(final InputStream stream) {
		return new InflaterInputStream(stream);
	}

	@Override
	public boolean compatible(final PushbackInputStream stream) throws IOException {
		final var array = stream.readNBytes(2);
		stream.unread(array);

		return compatible(array);
	}

	@Override
	public boolean compatible(final byte[] array) {
		if (array.length < 2) {
			return false;
		}

		final int value = (char) ArrayUtils.CHARS_BIG_ENDIAN.get(array, 0);

		// Check for compressor == 8 and FDICT == 0
		return (value & 0x0F20) == 0x0800 && value % 31 == 0;
	}
}
