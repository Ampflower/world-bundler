package gay.ampflower.bundler.compress;

import java.io.*;

/**
 * @author Ampflower
 * @since ${version}
 **/
public interface Compressor {

	OutputStream deflater(final OutputStream stream) throws IOException;

	InputStream inflater(final InputStream stream) throws IOException;

	boolean compatible(final PushbackInputStream stream) throws IOException;

	boolean compatible(final byte[] array);

	default boolean isCompressor() {
		return true;
	}

	default byte[] deflate(byte[] array) throws IOException {
		return this.deflate(array, 0, array.length);
	}

	default byte[] deflate(byte[] array, int off, int len) throws IOException {
		final var output = new ByteArrayOutputStream();
		try (final var stream = this.deflater(output)) {
			stream.write(array, off, len);
		}
		return output.toByteArray();
	}

	default byte[] inflate(byte[] array) throws IOException {
		return this.inflate(array, 0, array.length);
	}

	default byte[] inflate(byte[] array, int off, int len) throws IOException {
		try (final var input = new ByteArrayInputStream(array, off, len);
			  final var stream = this.inflater(input)) {

			return stream.readAllBytes();
		}
	}

	static Compressor getMcRegionCompressor(int compressor) {
		return CompressorRegistry.vanilla.getMcRegion(compressor);
	}

	static Compressor getFileCompressor(PushbackInputStream stream) throws IOException {
		final var array = stream.readNBytes(8);
		stream.unread(array);

		return getFileCompressor(array);
	}

	static Compressor getFileCompressor(byte[] array) {
		for (final var compressor : CompressorRegistry.vanilla.fileCompressors) {
			if (compressor.compatible(array)) {
				return compressor;
			}
		}

		return NoneCompressor.INSTANCE;
	}

	static byte[] tryDecompress(byte[] array) throws IOException {
		return getFileCompressor(array).inflate(array);
	}
}
