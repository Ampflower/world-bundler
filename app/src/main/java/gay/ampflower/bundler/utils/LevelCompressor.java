package gay.ampflower.bundler.utils;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import gay.ampflower.bundler.world.McRegionHandler;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
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
	;

	private static final Logger logger = LogUtils.logger();
	private static final Int2ObjectMap<LevelCompressor> mcRegionCompressors;

	static {
		final var mrc = new Int2ObjectArrayMap<LevelCompressor>();
		for (var compressor : values()) {
			if (compressor.MCREGION_TYPE < 0) continue;
			mrc.put(compressor.MCREGION_TYPE, compressor);
		}
		mcRegionCompressors = Int2ObjectMaps.unmodifiable(mrc);
	}

	public final byte MCREGION_TYPE;

	LevelCompressor(int mcRegionType) {
		this.MCREGION_TYPE = (byte) mcRegionType;
	}


	public static LevelCompressor getMcRegionCompressor(int compressor) {
		return mcRegionCompressors.get(compressor);
	}

	public abstract OutputStream deflater(OutputStream stream) throws IOException;

	public abstract InputStream inflater(InputStream stream) throws IOException;

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
