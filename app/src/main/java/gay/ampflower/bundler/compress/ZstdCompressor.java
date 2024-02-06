package gay.ampflower.bundler.compress;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import gay.ampflower.bundler.utils.ArrayUtils;
import gay.ampflower.bundler.utils.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.Arrays;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class ZstdCompressor implements Compressor {
	public static final ZstdCompressor INSTANCE = new ZstdCompressor();
	private static final Logger logger = LogUtils.logger();

	static final int magic = Zstd.magicNumber();

	@Override
	public OutputStream deflater(final OutputStream stream) throws IOException {
		return new ZstdOutputStream(stream);
	}

	@Override
	public InputStream inflater(final InputStream stream) throws IOException {
		return new ZstdInputStream(stream);
	}

	@Override
	public boolean compatible(final PushbackInputStream stream) throws IOException {
		final var array = stream.readNBytes(4);
		stream.unread(array);

		return compatible(array);
	}

	@Override
	public boolean compatible(final byte[] array) {
		if (array.length < 4) {
			return false;
		}

		final int value = (int) ArrayUtils.INTS_LITTLE_ENDIAN.get(array, 0);

		return value == magic;
	}

	@Override
	public byte[] deflate(final byte[] array, final int off, final int len) {
		final long size = Zstd.compressBound(len);
		if (size > Integer.MAX_VALUE || size < 0)
			throw new OutOfMemoryError("cannot allocate" + Long.toUnsignedString(size) + " byte array");
		final var buf = new byte[(int) size];
		final long size2 = Zstd.compressByteArray(buf, 0, (int) size, array, off, len, 21);
		return Arrays.copyOfRange(buf, 0, (int) size2);
	}

	@Override
	public byte[] inflate(final byte[] array, final int off, final int len) throws IOException {
		final long size = Zstd.decompressedSize(array, off, len);
		if (size == -1L) {
			// Seems that the stream deflater doesn't store sizing and emits -1.
			logger.trace("Got -1 for {}[{}:{}] ({}); streamed?", array, off, off + len, System.identityHashCode(array));
			return Compressor.super.inflate(array, off, len);
		}
		if (size > Integer.MAX_VALUE || size < 0) {
			throw new OutOfMemoryError("cannot allocate " + Long.toUnsignedString(size) + " byte array (" + Long.toHexString(size) + ')');
		}
		final var ret = new byte[(int) size];
		final long pass2 = Zstd.decompressByteArray(ret, 0, (int) size, array, off, len);
		if (pass2 != size) logger.warn("{} != {} for Zstd.inflate({}, {}, {})", size, pass2, array, off, len);
		return ret;
	}
}
