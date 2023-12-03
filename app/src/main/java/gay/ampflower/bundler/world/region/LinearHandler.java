package gay.ampflower.bundler.world.region;

import com.github.luben.zstd.Zstd;
import gay.ampflower.bundler.utils.ArrayUtils;
import gay.ampflower.bundler.utils.LevelCompressor;
import gay.ampflower.bundler.utils.LimitedInputStream;
import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.world.Chunk;
import gay.ampflower.bundler.world.Region;
import gay.ampflower.bundler.world.io.RegionHandler;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Set;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class LinearHandler implements RegionHandler {
	private static final Logger logger = LogUtils.logger();

	public static final LinearHandler INSTANCE = new LinearHandler();

	// Linear uses Big Endian
	private static final VarHandle SHORT_HANDLE = ArrayUtils.SHORTS_BIG_ENDIAN;
	private static final VarHandle INT_HANDLE = ArrayUtils.INTS_BIG_ENDIAN;
	private static final VarHandle LONG_HANDLE = ArrayUtils.LONGS_BIG_ENDIAN;

	private static final long LINEAR_SIGNATURE = 0xC3FF13183CCA9D9AL;
	private static final byte LINEAR_VERSION = 1;
	// Supported versions.
	private static final Set<Byte> LINEAR_VERSIONS = Set.of((byte) 1, (byte) 2);
	private static final int HEADER_LENGTH = 32;


	public Region readRegion(final int x, final int y, InputStream stream) throws IOException {
		final var buf = new byte[8192];
		int read = 0;

		final var header = readHeader(stream, buf);

		final var timestamps = new int[Region.CHUNK_COUNT];
		final var chunks = new byte[Region.CHUNK_COUNT][];

		try (
			final var limitStream = new LimitedInputStream(stream, header.completeRegionLength());
			final var zstdStream = new ZstdCompressorInputStream(limitStream)) {
			final int metaSize = Region.CHUNK_COUNT * 2;
			final var chunkMeta = new int[metaSize];

			if((read = zstdStream.readNBytes(buf, 0, metaSize * 4)) != metaSize * 4) {
				throw new IOException("Expected " + metaSize * 4 + ", got " + read);
			}
			// read += IoUtils.readMin(zstdStream, buf, read, metaSize * 4, read - HEADER_LENGTH);
			ArrayUtils.copy(buf, 0, chunkMeta, 0, metaSize, ByteOrder.BIG_ENDIAN);

			int chunkCount = 0;

			for(int i = 0; i < Region.CHUNK_COUNT; i++) {
				final int currentChunk = i * 2;
				final int chunkSize = chunkMeta[currentChunk];

				if(chunkSize != 0) {
					chunkCount++;

					timestamps[i] = chunkMeta[currentChunk + 1];
					chunks[i] = readChunk(zstdStream, i, chunkSize);
				}
			}

			if(chunkCount != header.chunkCount()) {
				logger.warn("Missing chunks detected, expected {}, got {}", header.chunkCount, chunkCount);
			}
		}

		read = stream.read(buf, 0, 8);

		if(read != 8 || (long)LONG_HANDLE.get(buf, 0) != LINEAR_SIGNATURE) {
			logger.warn("Missing trailer, got {}", read);
		}

		return new Region(x, y, timestamps, chunks);
	}

	private static Header readHeader(InputStream stream, byte[] buf) throws IOException {
		final Header header;
		final int read;

		if((read = stream.readNBytes(buf, 0, HEADER_LENGTH)) != HEADER_LENGTH) {
			throw new IOException("Region corrupted or bad stream? Read " + read + " bytes.");
		}

		header = new Header(buf);

		if(header.signature() != LINEAR_SIGNATURE) {
			throw new IOException("Invalid signature, got " + Long.toHexString(header.signature()));
		}

		if(!LINEAR_VERSIONS.contains(header.version())) {
			throw new IOException("Unsupported version, got " + header.version());
		}

		if(header.completeChunkHash() != 0x00L) {
			logger.warn("Unexpected chunk hash, got {}; you may be dealing with an updated version of Linear",
				Long.toHexString(header.completeChunkHash()));
		}

		return header;
	}

	private static byte[] readChunk(InputStream stream, int i, int chunkSize) throws IOException {
		final var chunkRaw = stream.readNBytes(chunkSize);

		if(chunkSize != chunkRaw.length) {
			throw new IOException("Unexpected EOF at chunk "+i+": Expected size "+chunkSize+", got "+chunkRaw.length);
		}
		if(chunkRaw[0] != 0x0A) {
			logger.warn("Potentially invalid chunk at {}: Got {} at NBT starting point", i, chunkRaw[0]);
		}
		if(chunkRaw[chunkSize - 1] != 0x00) {
			logger.warn("Potentially invalid chunk at {}: Got {} at end of chunk", i, chunkRaw[chunkSize - 1]);
		}
		final short name = (short)SHORT_HANDLE.get(chunkRaw, 1);
		if(name != 0x0000) {
			logger.warn("Potentially invalid chunk at {}: Name length {}, normally 0 in Vanilla", i, name);
		}

		return chunkRaw;
	}

	public void writeRegion(OutputStream stream, Region region) throws IOException {
		final int compressionLevel = Zstd.maxCompressionLevel();

		final var chunks = writeChunks(region);

		final var header = new Header(
			LINEAR_SIGNATURE,
			LINEAR_VERSION,
			ArrayUtils.maxToInt(region.chunks(), Chunk::timestamp),
			(byte) compressionLevel,
			(short) chunks.count(),
			chunks.array().length,
			0L
		);

		var array = header.toBytes();

		stream.write(array);
		stream.write(chunks.array());
		stream.write(array, 0, ArrayUtils.LONG_STRIDE);
	}

	private Chunks writeChunks(Region region) throws IOException {
		final int[] chunkMeta = new int[Region.CHUNK_COUNT * 2];

		int size = 0, count = 0;

		for (int i = 0; i < Region.CHUNK_COUNT; i++) {
			int metaIndex = i * 2;

			var chunk = region.chunks()[i];
			if (chunk == null || chunk.array().length == 0) {
				continue;
			}

			count++;
			size += chunkMeta[metaIndex] = chunk.array().length;
			chunkMeta[metaIndex + 1] = chunk.timestamp();
		}

		int offset = chunkMeta.length * ArrayUtils.INT_STRIDE;
		final byte[] bytes = new byte[size + offset];
		ArrayUtils.copy(chunkMeta, 0, bytes, 0, chunkMeta.length, ByteOrder.BIG_ENDIAN);

		for (var chunk : region.chunks()) {
			if (chunk == null || chunk.array().length == 0) continue;
			System.arraycopy(chunk.array(), 0, bytes, offset, chunk.array().length);
			offset += chunk.array().length;
		}

		return new Chunks(count, LevelCompressor.ZSTD.deflate(bytes));
	}

	private record Header(
		long signature,
		byte version,
		long newestTimestamp,
		byte compressionLevel,
		short chunkCount,
		int completeRegionLength,
		// Currently reserved, keep at 0x0000_0000_0000_0000L
		@Deprecated
		long completeChunkHash
	) {
		private Header(byte[] bytes) {
			this(
				(long) LONG_HANDLE.get(bytes, 0),
				bytes[8],
				(long) LONG_HANDLE.get(bytes, 9),
				bytes[17],
				(short) SHORT_HANDLE.get(bytes, 18),
				(int) INT_HANDLE.get(bytes, 20),
				(long) LONG_HANDLE.get(bytes, 24)
			);
		}

		public byte[] toBytes() {
			final var bytes = new byte[32];
			LONG_HANDLE.set(bytes, 0, signature);
			bytes[8] = version;
			LONG_HANDLE.set(bytes, 9, newestTimestamp);
			bytes[17] = compressionLevel;
			SHORT_HANDLE.set(bytes, 18, chunkCount);
			INT_HANDLE.set(bytes, 20, completeRegionLength);
			LONG_HANDLE.set(bytes, 24, completeChunkHash);
			return bytes;
		}
	}

	private record Footer(
		long signature
	) {
		private Footer(byte[] bytes) {
			this((long) LONG_HANDLE.get(bytes, bytes.length - 8));
		}
	}

	private record ChunkEntry (
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

	private record Chunks(
		int count,
		byte[] array
	) {
	}
}
