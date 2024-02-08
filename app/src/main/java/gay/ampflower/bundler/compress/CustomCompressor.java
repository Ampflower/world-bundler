package gay.ampflower.bundler.compress;

import gay.ampflower.bundler.utils.Identifier;
import gay.ampflower.bundler.utils.io.DataUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class CustomCompressor implements Compressor {
	public static final CustomCompressor INSTANCE = new CustomCompressor();

	@Override
	public OutputStream deflater(final OutputStream stream) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public InputStream inflater(final InputStream stream) throws IOException {
		final var type = DataUtils.readString(stream);
		final var compressor = CompressorRegistry.vanilla.get(Identifier.ofMinecraft(type));

		if (compressor == null) {
			throw new IOException("unknown compressor " + type);
		}

		return compressor.inflater(stream);
	}

	@Override
	public boolean compatible(final PushbackInputStream stream) throws IOException {
		final var sizeBytes = stream.readNBytes(2);

		if (sizeBytes.length < 2) {
			stream.unread(sizeBytes);
			return false;
		}

		final int size = DataUtils.readStringSize(sizeBytes, 0);
		final var array = stream.readNBytes(size);

		stream.unread(array);
		stream.unread(sizeBytes);

		if (array.length < size) {
			return false;
		}

		final var type = DataUtils.readString(array, 0, size);
		return CompressorRegistry.vanilla.get(Identifier.ofMinecraft(type)) != null;
	}

	@Override
	public boolean compatible(final byte[] array) {
		if (array.length < 2) {
			return false;
		}
		final int size = DataUtils.readStringSize(array, 0);
		if (array.length - 2 < size) {
			return false;
		}
		final var type = DataUtils.readString(array, 2, size);
		return CompressorRegistry.vanilla.get(Identifier.ofMinecraft(type)) != null;
	}
}
