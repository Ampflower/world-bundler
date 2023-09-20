package gay.ampflower.bundler.world;

import gay.ampflower.bundler.utils.IoUtils;
import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.world.io.ChunkWriter;
import gay.ampflower.bundler.world.io.RegionHandler;
import gay.ampflower.bundler.world.io.RegionReader;
import gay.ampflower.bundler.world.io.RegionWriter;
import org.slf4j.Logger;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.Deflater;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class McRegionHandler implements RegionHandler {
	private static final Logger logger = LogUtils.logger();

	public static final McRegionHandler INSTANCE = new McRegionHandler();

	// BE is used for McRegion.
	private static final VarHandle INT_HANDLE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);

	private static final int BYTE_MASK = 0xFF;
	private static final int SECTOR = 4096;
	private static final int SECTOR_MASK = SECTOR - 1;
	private static final int SECTOR_BITS = 12;

	private static final int INITIAL_SECTOR_OFFSET = 2;
	private static final int CHUNK_HEADER_SIZE = 5;

	private static final int MAX_SECTORS = 255;
	private static final int CHUNK_CUTOFF = SECTOR * MAX_SECTORS;

	// Fun fact: 0x00 is unused.
	public static final byte COMPRESSION_GZIP = 0x01;
	public static final byte COMPRESSION_ZLIB = 0x02;
	public static final byte COMPRESSION_NONE = 0x03;

	private static final byte COMPRESSION_FLAG_EXTERN = -0x80;

	@Override
	public Region readRegion(final InputStream stream) throws IOException {
		return null;
	}

	@Override
	public void writeRegion(final OutputStream stream, final Region region) throws IOException {
		this.writeRegion(stream, region, new ChunkWriter.McLogger());
	}

	@Override
	public void writeRegion(final OutputStream stream, final Region region, final ChunkWriter chunkWriter) throws IOException {
		final var entries = new ChunkEntry[Region.CHUNK_COUNT];
		final var sizes = new int[Region.CHUNK_COUNT];
		final var chunks = compressChunks(region, sizes,  entries);

		final var buf = new byte[SECTOR];

		for(int i = 0; i < Region.CHUNK_COUNT; i++) {
			INT_HANDLE.set(buf, i * 4, entries[i].toUpper());
		}
		stream.write(buf);

		for(int i = 0; i < Region.CHUNK_COUNT; i++) {
			INT_HANDLE.set(buf, i * 4, entries[i].toLower());
		}
		stream.write(buf);

		for(int i = 0; i < Region.CHUNK_COUNT; i++) {
			if(entries[i].sectors < 0) {
				INT_HANDLE.set(buf, 0, 1);
				if(chunkWriter != null) {
					chunkWriter.writeChunk(i, chunks[i]);
					buf[4] = COMPRESSION_ZLIB;
				} else {
					buf[4] = COMPRESSION_NONE;
				}
				Arrays.fill(buf, 5, SECTOR, (byte) 0);
				stream.write(buf);
				continue;
			}
			if(entries[i].sectors == 0) {
				continue;
			}
			// Apparently includes the compression byte.
			INT_HANDLE.set(buf, 0, sizes[i] + 1);
			buf[4] = COMPRESSION_ZLIB;

			IoUtils.writeSectors(stream, chunks[i], 0, buf, CHUNK_HEADER_SIZE, sizes[i]);
			stream.flush();
		}
	}

	public static int sectors(int i) {
		if(i == 0) {
			return 0;
		}
		// Accounts for header;
		i += CHUNK_HEADER_SIZE;
		return (i >> SECTOR_BITS) + ((i & (SECTOR_MASK)) != 0 ? 1 : 0);
	}

	@CheckReturnValue
	private static byte[][] compressChunks(final Region region, final int[] sizes, final ChunkEntry[] entries) {
		final var deflater = new Deflater(Deflater.BEST_COMPRESSION);
		final var write = new byte[Region.CHUNK_COUNT][];
		int offset = INITIAL_SECTOR_OFFSET;

		for(int i = 0; i < Region.CHUNK_COUNT; i++) {
			final var chunk = region.chunks()[i];
			if(chunk != null) {
				IoUtils.verifyNbt(chunk);
				deflater.setInput(chunk);
				deflater.finish();
				final var toWrite = new byte[sectors(chunk.length) * SECTOR];
				final var toSize = deflater.deflate(toWrite);
				if(!deflater.finished()) {
					logger.warn("Discarded {} as : {}", i, toWrite);
					sizes[i] = 0;
				} else {
					write[i] = toWrite;
					sizes[i] = toSize;
				}
				deflater.reset();
			}

			int sectors = sectors(sizes[i]);
			if(sectors == 0) {
				assert (sizes[i] == 0);
				entries[i] = ChunkEntry.SENTINEL;
			} else if (sectors > MAX_SECTORS) {
				entries[i] = new ChunkEntry(offset, -1, region.timestamps()[i]);
				logger.info("Signaling extern: {}", i);
				offset++;
			} else {
				entries[i] = new ChunkEntry(offset, sectors, region.timestamps()[i]);
				logger.info("Preparing {} -> {}", offset, sectors);
				offset += sectors;
			}
		}

		deflater.end();

		return write;
	}

	public final class Reader implements RegionReader {
		private final ChunkEntry[] entries = new ChunkEntry[1024];
	}

	public final class Writer implements RegionWriter {
		private final ChunkEntry[] entries = new ChunkEntry[1024];

		@Override
		public ChunkWriter nextChunk() {
			return null;
		}
	}


	private record ChunkEntry(int offset, int sectors, int timestamp) {
		public ChunkEntry(int upper, int lower) {
			this(upper >>> 8, upper & BYTE_MASK, lower);
		}

		int toUpper() {
			if(sectors < 0) {
				return (offset << 8) | 1;
			}
			return (offset << 8) | sectors;
		}

		int toLower() {
			return timestamp;
		}

		private static final ChunkEntry SENTINEL = new ChunkEntry(0, 0, 0);
	}
}
