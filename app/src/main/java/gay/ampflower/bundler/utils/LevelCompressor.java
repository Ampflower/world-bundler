package gay.ampflower.bundler.utils;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import gay.ampflower.bundler.world.region.McRegionHandler;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Factory;
import org.slf4j.Logger;

import java.io.*;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * @author Ampflower
 * @since ${version}
 **/
public enum LevelCompressor {
	NONE(McRegionHandler.COMPRESSION_NONE) {
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
	},
	GZIP(McRegionHandler.COMPRESSION_GZIP) {
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
	},
	ZLIB(McRegionHandler.COMPRESSION_ZLIB) {
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
	},
	ZSTD(-1) {
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

			final int value = (int) ArrayUtils.INTS_BIG_ENDIAN.get(array, 0);

			return value == 0xFD2FB528;
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
		public byte[] inflate(final byte[] array, final int off, final int len) {
			final long size = Zstd.decompressedSize(array, off, len);
			if (size > Integer.MAX_VALUE || size < 0)
				throw new OutOfMemoryError("cannot allocate " + Long.toUnsignedString(size) + " byte array");
			final var ret = new byte[(int) size];
			final long pass2 = Zstd.decompressByteArray(ret, 0, (int) size, array, off, len);
			if (pass2 != size) logger.warn("{} != {} for Zstd.inflate({}, {}, {})", size, pass2, array, off, len);
			return ret;
		}
	},
	LZ4(McRegionHandler.COMPRESSION_LZ4) {
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
	},
	;

	private static final Logger logger = LogUtils.logger();
	private static final Int2ObjectMap<LevelCompressor> mcRegionCompressors;
	private static final LevelCompressor[] fileCompressors;

	static {
		final var self = values();
		final var fc = new LevelCompressor[self.length];
		int fci = 0;
		final var mrc = new Int2ObjectArrayMap<LevelCompressor>();

		for (var compressor : self) {
			if (compressor.isCompressor()) {
				fc[fci++] = compressor;
			}

			if (compressor.MCREGION_TYPE < 0) continue;
			mrc.put(compressor.MCREGION_TYPE, compressor);
		}
		mcRegionCompressors = Int2ObjectMaps.unmodifiable(mrc);
		fileCompressors = Arrays.copyOf(fc, fci);
	}

	public final byte MCREGION_TYPE;

	LevelCompressor(int mcRegionType) {
		this.MCREGION_TYPE = (byte) mcRegionType;
	}


	public static LevelCompressor getMcRegionCompressor(int compressor) {
		return mcRegionCompressors.get(compressor);
	}

	public static LevelCompressor getFileCompressor(PushbackInputStream stream) throws IOException {
		final var array = stream.readNBytes(8);
		stream.unread(array);

		return getFileCompressor(array);
	}

	public static LevelCompressor getFileCompressor(byte[] array) {
		for (final var compressor : fileCompressors) {
			if (compressor.compatible(array)) {
				return compressor;
			}
		}

		return NONE;
	}

	public static byte[] tryDecompress(byte[] array) throws IOException {
		return getFileCompressor(array).inflate(array);
	}

	public abstract OutputStream deflater(OutputStream stream) throws IOException;

	public abstract InputStream inflater(InputStream stream) throws IOException;

	public abstract boolean compatible(PushbackInputStream stream) throws IOException;

	public abstract boolean compatible(byte[] array);

	public boolean isCompressor() {
		return true;
	}

	public byte[] deflate(byte[] array) throws IOException {
		return this.deflate(array, 0, array.length);
	}

	public byte[] deflate(byte[] array, int off, int len) throws IOException {
		final var output = new ByteArrayOutputStream();
		try (final var stream = this.deflater(output)) {
			stream.write(array, off, len);
		}
		return output.toByteArray();
	}

	public byte[] inflate(byte[] array) throws IOException {
		return this.inflate(array, 0, array.length);
	}

	public byte[] inflate(byte[] array, int off, int len) throws IOException {
		try (final var input = new ByteArrayInputStream(array, off, len);
			  final var stream = this.inflater(input)) {

			return stream.readAllBytes();
		}
	}
}
