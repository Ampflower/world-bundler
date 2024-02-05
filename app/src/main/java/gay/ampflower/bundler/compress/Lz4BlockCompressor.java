package gay.ampflower.bundler.compress;

import gay.ampflower.bundler.utils.ArrayUtils;
import gay.ampflower.bundler.utils.SizeUtils;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Factory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class Lz4BlockCompressor implements Compressor {
	public static final Lz4BlockCompressor INSTANCE = new Lz4BlockCompressor();

	private final LZ4Factory factory = LZ4Factory.fastestInstance();

	@Override
	public OutputStream deflater(final OutputStream stream) throws IOException {
		return new LZ4BlockOutputStream(stream, (int) SizeUtils.MiB * 32, factory.highCompressor());
	}

	@Override
	public InputStream inflater(final InputStream stream) throws IOException {
		return new LZ4BlockInputStream(stream, factory.fastDecompressor());
	}

	@Override
	public boolean compatible(final PushbackInputStream stream) throws IOException {
		final byte[] bytes = stream.readNBytes(8);
		stream.unread(bytes);
		return compatible(bytes);
	}

	@Override
	public boolean compatible(final byte[] array) {
		if (array.length < 8) {
			return false;
		}
		final long value = (long) ArrayUtils.LONGS_BIG_ENDIAN.get(array, 0);
		return value == 0x4c5a_3442_6c6f_636bL;
	}
}
