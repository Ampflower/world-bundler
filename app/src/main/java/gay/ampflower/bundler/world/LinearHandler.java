package gay.ampflower.bundler.world;

import gay.ampflower.bundler.utils.IoUtils;
import gay.ampflower.bundler.utils.LimitedInputStream;
import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.world.io.RegionHandler;
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream;
import org.apache.commons.compress.compressors.zstandard.ZstdUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Set;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class LinearHandler implements RegionHandler {
	private static final Logger logger = LogUtils.logger();

	public static final LinearHandler INSTANCE = new LinearHandler();

	// Linear uses Big Endian
	private static final VarHandle SHORT_HANDLE = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.BIG_ENDIAN);
	private static final VarHandle INT_HANDLE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);
	private static final VarHandle LONG_HANDLE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);

	private static final long LINEAR_SIGNATURE = 0xC3FF13183CCA9D9AL;
	// Supported versions.
	private static final Set<Byte> LINEAR_VERSIONS = Set.of((byte)1, (byte)2);
	private static final int HEADER_LENGTH = 32;


	public Region readRegion(InputStream stream) throws IOException {
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
			IoUtils.copy(buf, 0, chunkMeta, 0, metaSize, ByteOrder.BIG_ENDIAN);

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

		return new Region(timestamps, chunks);
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
		;
	}

	private final int[] chunkSizes = new int[Region.CHUNK_COUNT];

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
				(long)LONG_HANDLE.get(bytes, 0),
				bytes[8],
				(long)LONG_HANDLE.get(bytes, 9),
				bytes[17],
				(short)SHORT_HANDLE.get(bytes, 18),
				(int)INT_HANDLE.get(bytes, 20),
				(long)LONG_HANDLE.get(bytes, 24)
			);
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
	){
		private ChunkEntry(byte[] bytes, int offset) {
			this(
				(int)INT_HANDLE.get(bytes, offset),
				(int)INT_HANDLE.get(bytes, offset + 4)
			);
		}
	}
}
