package gay.ampflower.bundler.compress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class GZipCompressor implements Compressor {
	public static final GZipCompressor INSTANCE = new GZipCompressor();

	@Override
	public OutputStream deflater(final OutputStream stream) throws IOException {
		return new GZIPOutputStream(stream);
	}

	@Override
	public InputStream inflater(final InputStream stream) throws IOException {
		return new GZIPInputStream(stream);
	}

	@Override
	public boolean compatible(final PushbackInputStream stream) throws IOException {
		final var array = stream.readNBytes(2);
		stream.unread(array);

		return compatible(array);
	}

	@Override
	public boolean compatible(final byte[] array) {
		// Common GZip Header
		return array.length >= 2 && array[0] == 0x1F && array[1] == (byte) 0x8B;
	}
}
