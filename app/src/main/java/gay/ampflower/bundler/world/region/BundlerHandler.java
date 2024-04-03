package gay.ampflower.bundler.world.region;

import gay.ampflower.bundler.utils.ArrayUtils;
import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.utils.io.IoUtils;
import gay.ampflower.bundler.world.Chunk;
import gay.ampflower.bundler.world.Region;
import gay.ampflower.bundler.world.io.RegionHandler;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.VarHandle;
import java.util.zip.Adler32;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class BundlerHandler implements RegionHandler {
	private static final Logger logger = LogUtils.logger();

	public static final BundlerHandler INSTANCE = new BundlerHandler();

	private static final VarHandle SHORT_HANDLE = ArrayUtils.SHORTS_BIG_ENDIAN;
	private static final VarHandle INT_HANDLE = ArrayUtils.INTS_BIG_ENDIAN;

	private static final char BUNDLER_SIGNATURE = '⚘';
	private static final byte BUNDLER_VERSION = 0;
	private static final int HEADER_LENGTH = 4;


	public Region readRegion(final int x, final int y, InputStream stream) throws IOException {
		final var buf = new byte[8192];
		final var header = readHeader(stream, buf);
		int chunkCount = 0;
		final var chunks = new Chunk[Region.CHUNK_COUNT];
		final var adler = new Adler32();

		for (int i = 0; i < Region.CHUNK_COUNT; i++) {
			final int size = IoUtils.readIntBigEndian(stream, buf, 0);

			if (size < 0) {
				logger.trace("{} + {} ({})", i, ~size, Integer.toUnsignedString(size, 16));
				i += ~size;
			}
			if (size <= 0) {
				logger.trace("Jumping as size <= 0");
				continue;
			}

			final int timestamp = IoUtils.readIntBigEndian(stream, buf, 0);
			final byte[] chunk = readChunk(stream, i, size);

			adler.update(chunk);

			final int real = (int) adler.getValue();
			adler.reset();
			logger.trace("Read {} bytes @ {} with timestamp {} & chksum {}", chunk.length, i, timestamp, Integer.toUnsignedString(real, 16));

			chunkCount++;
			chunks[i] = new Chunk(x, y, i, timestamp, chunk);
		}

		final int expectedChunkCount = IoUtils.readIntBigEndian(stream, buf, 0);

		if (chunkCount != expectedChunkCount) {
			logger.warn("Missing chunks detected, expected {}, got {}", expectedChunkCount, chunkCount);
		}

		if (chunkCount == 0) {
			return new Region(x, y);
		}

		return new Region(x, y, chunks);
	}

	private static Header readHeader(InputStream stream, byte[] buf) throws IOException {
		final Header header;
		final int read;

		if ((read = stream.readNBytes(buf, 0, HEADER_LENGTH)) != HEADER_LENGTH) {
			throw new IOException("Region corrupted or bad stream? Read " + read + " bytes.");
		}

		header = new Header(buf);

		if (header.signature() != BUNDLER_SIGNATURE) {
			throw new IOException("Invalid signature, got " + Long.toHexString(header.signature()));
		}

		if (header.version() != BUNDLER_VERSION) {
			throw new IOException("Unsupported version, got " + header.version());
		}

		return header;
	}

	private static byte[] readChunk(InputStream stream, int i, int chunkSize) throws IOException {
		final var chunkRaw = stream.readNBytes(chunkSize);

		if (chunkSize != chunkRaw.length) {
			throw new IOException("Unexpected EOF at chunk " + i + ": Expected size " + chunkSize + ", got " + chunkRaw.length);
		}
		if (chunkRaw[0] != 0x0A) {
			logger.warn("Potentially invalid chunk at {}: Got {} at NBT starting point", i, chunkRaw[0]);
		}
		if (chunkRaw[chunkSize - 1] != 0x00) {
			logger.warn("Potentially invalid chunk at {}: Got {} at end of chunk", i, chunkRaw[chunkSize - 1]);
		}
		final short name = (short) SHORT_HANDLE.get(chunkRaw, 1);
		if (name != 0x0000) {
			logger.warn("Potentially invalid chunk at {}: Name length {}, normally 0 in Vanilla", i, name);
		}

		return chunkRaw;
	}

	public void writeRegion(OutputStream stream, Region region) throws IOException {
		final var header = new Header(
			(short) BUNDLER_SIGNATURE,
			BUNDLER_VERSION,
			(byte) 0
		);

		var array = header.toBytes();

		stream.write(array);
		writeChunks(stream, region);
	}

	private void writeChunks(OutputStream stream, Region region) throws IOException {
		final byte[] buf = new byte[8];

		final var adler = new Adler32();

		int count = 0, last = -1;

		for (int i = 0; i < Region.CHUNK_COUNT; i++) {
			var chunk = region.chunks()[i];
			if (chunk == null || chunk.isEmpty()) {
				continue;
			}

			int delta = i - last;
			if (delta > 1) {
				delta--;
				INT_HANDLE.set(buf, 0, -delta);
				stream.write(buf, 0, ArrayUtils.INT_STRIDE);
				logger.trace("Wrote delta {} as {} ({})", delta, Integer.toHexString(-delta), -delta);
			}
			last = i;

			final var data = chunk.array();

			INT_HANDLE.set(buf, 0, data.length);
			INT_HANDLE.set(buf, 4, chunk.timestamp());

			count++;

			stream.write(buf);
			stream.write(data);

			adler.update(data);
			int adlerValue = (int) adler.getValue();
			INT_HANDLE.set(buf, 0, adlerValue);
			adler.reset();

			logger.trace("Wrote {} bytes @ {} with timestamp {} & chksum {}", data.length, i, chunk.timestamp(), Integer.toUnsignedString(adlerValue, 16));
		}
		int delta = Region.CHUNK_COUNT - last;
		if (delta > 1) {
			INT_HANDLE.set(buf, 0, -delta);
			stream.write(buf, 0, ArrayUtils.INT_STRIDE);
		}

		INT_HANDLE.set(buf, 0, count);
		stream.write(buf, 0, ArrayUtils.INT_STRIDE);
	}

	private record Header(
		short signature,
		byte version,
		@Deprecated
		byte reserved
	) {
		private Header(byte[] bytes) {
			this(
				(short) SHORT_HANDLE.get(bytes, 0),
				bytes[2],
				bytes[3]
			);
		}

		public byte[] toBytes() {
			final var bytes = new byte[4];
			SHORT_HANDLE.set(bytes, 0, signature);
			bytes[2] = version;
			bytes[3] = reserved;
			return bytes;
		}
	}

	private record ChunkEntry(
		int size,
		int timestamp
	) {
		private ChunkEntry(byte[] bytes, int offset) {
			this(
				(int) INT_HANDLE.get(bytes, offset),
				(int) INT_HANDLE.get(bytes, offset + 4)
			);
		}
	}
}
