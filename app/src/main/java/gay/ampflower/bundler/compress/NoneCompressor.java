package gay.ampflower.bundler.compress;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.Arrays;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class NoneCompressor implements Compressor {
	public static final NoneCompressor INSTANCE = new NoneCompressor();

	@Override
	public OutputStream deflater(final OutputStream stream) {
		return stream;
	}

	@Override
	public InputStream inflater(final InputStream stream) {
		return stream;
	}

	@Override
	public boolean compatible(final PushbackInputStream stream) {
		return true;
	}

	@Override
	public boolean compatible(final byte[] array) {
		return true;
	}

	@Override
	public boolean isCompressor() {
		return false;
	}

	@Override
	public byte[] deflate(final byte[] array) {
		return array;
	}

	@Override
	public byte[] deflate(final byte[] array, final int off, final int len) {
		return Arrays.copyOfRange(array, off, off + len);
	}

	@Override
	public byte[] inflate(final byte[] array) {
		return array;
	}

	@Override
	public byte[] inflate(final byte[] array, final int off, final int len) {
		return Arrays.copyOfRange(array, off, off + len);
	}
}
