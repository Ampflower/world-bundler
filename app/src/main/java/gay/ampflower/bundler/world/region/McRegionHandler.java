package gay.ampflower.bundler.world.region;

import gay.ampflower.bundler.utils.ArrayUtils;
import gay.ampflower.bundler.utils.IoUtils;
import gay.ampflower.bundler.utils.LevelCompressor;
import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.world.Region;
import gay.ampflower.bundler.world.io.ChunkReader;
import gay.ampflower.bundler.world.io.ChunkWriter;
import gay.ampflower.bundler.world.io.RegionHandler;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import org.slf4j.Logger;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.VarHandle;
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
	private static final VarHandle INT_HANDLE = ArrayUtils.INTS_BIG_ENDIAN;

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
	public Region readRegion(final int x, final int y, final InputStream stream) throws IOException {
		return this.readRegion(x, y, stream, new ChunkReader.McLogger());
	}

	@Override
	public Region readRegion(final int x, final int y, final InputStream stream, final ChunkReader chunkReader) throws IOException {
		int sectorOffset = INITIAL_SECTOR_OFFSET;
		final var buf = new byte[SECTOR];

		final var entries = readFirstSector(stream, buf);

		int l = 0;

		final var set = new IntOpenHashSet(Region.CHUNK_COUNT);

		for (int i = 0; i < Region.CHUNK_COUNT; i++) {
			final var entry = entries[i];
			if (entry == FirstSectorEntry.SENTINEL) {
				continue;
			}
			if (entry.offset == 1) {
				logger.info("Legacy McRegion detected; timestamps won't be saved.");
				sectorOffset = 1;
			}
			if (!set.add(entry.offset)) {
				logger.warn("Corrupted chunk entry {}: {}", i, entry);
			}
			l = Math.max(entry.offset + entry.sectors, l);
		}
		if (l <= sectorOffset) {
			return null;
		}

		final int[] timestamps;

		if (sectorOffset == INITIAL_SECTOR_OFFSET) {
			timestamps = IoUtils.readBigEndian(stream, buf);
		} else {
			timestamps = new int[Region.CHUNK_COUNT];
		}

		final var chunks = new byte[Region.CHUNK_COUNT][];
		final var bytes = stream.readNBytes((l - sectorOffset) * SECTOR);

		for (int i = 0; i < Region.CHUNK_COUNT; i++) {
			final var entry = entries[i];
			if (entry == FirstSectorEntry.SENTINEL) {
				continue;
			}

			int offset = (entry.offset() - sectorOffset) * SECTOR;
			int size = (int) INT_HANDLE.get(bytes, offset) - 1;

			int compressorId = bytes[offset + 4];
			var compressor = LevelCompressor.getMcRegionCompressor(compressorId & 0x7F);

			if (compressor == null) {
				logger.warn("Corrupted chunk {}; Invalid compressor: {}", i, compressorId & 0x7F);
				continue;
			}

			if (compressorId < 0) {
				chunks[i] = chunkReader.readChunk(i, compressor);
				if (size != 0 && chunks[i] != null) logger.warn("Corrupted chunk {}; found size {}", i, size);
				if (chunks[i] != null) {
					IoUtils.verifyNbt(chunks[i], i);
				}
			} else if (size == 0) {
				logger.warn("Corrupted chunk {}; zero-size with compressor {}", i, compressorId & 0xFF);
			} else {
				chunks[i] = compressor.inflate(bytes, offset + 5, size);
				IoUtils.verifyNbt(chunks[i], i);
			}
		}

		return new Region(x, y, timestamps, chunks);
	}

	private static FirstSectorEntry[] readFirstSector(final InputStream stream, final byte[] buf) throws IOException {
		final int[] offsets = IoUtils.readBigEndian(stream, buf);

		final var entries = new FirstSectorEntry[Region.CHUNK_COUNT];

		for (int i = 0; i < Region.CHUNK_COUNT; i++) {
			entries[i] = FirstSectorEntry.of(offsets[i], i);
		}

		return entries;
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
			final int timestamp;
			if(chunk != null) {
				IoUtils.verifyNbt(chunk.array(), i);
				deflater.setInput(chunk.array());
				deflater.finish();
				final var toWrite = new byte[sectors(chunk.array().length) * SECTOR];
				final var toSize = deflater.deflate(toWrite);
				if (!deflater.finished()) {
					logger.warn("Discarded {} as : {}", i, toWrite);
					sizes[i] = 0;
				} else {
					write[i] = toWrite;
					sizes[i] = toSize;
				}
				deflater.reset();
				timestamp = chunk.timestamp();
			} else {
				timestamp = 0;
			}

			int sectors = sectors(sizes[i]);
			if(sectors == 0) {
				assert (sizes[i] == 0);
				entries[i] = ChunkEntry.SENTINEL;
			} else if (sectors > MAX_SECTORS) {
				entries[i] = new ChunkEntry(offset, -1, timestamp);
				logger.info("Signaling extern: {}", i);
				offset++;
			} else {
				entries[i] = new ChunkEntry(offset, sectors, timestamp);
				logger.info("Preparing {} -> {}", offset, sectors);
				offset += sectors;
			}
		}

		deflater.end();

		return write;
	}

	private record FirstSectorEntry(int offset, int sectors) {
		private static FirstSectorEntry of(int upper, int pos) {
			if (upper == 0) {
				return SENTINEL;
			}

			final int offset = upper >>> 8;
			final int sectors = upper & BYTE_MASK;

			if (sectors == 0 || offset == 0) {
				logger.warn("Corrupted entry (offset: {}, sectors: {}) {}", offset, sectors, pos);
				return SENTINEL;
			}

			return new FirstSectorEntry(upper >>> 8, upper & BYTE_MASK);
		}

		private static final FirstSectorEntry SENTINEL = new FirstSectorEntry(0, 0);
	}

	private record ChunkEntry(int offset, int sectors, int timestamp) {
		public ChunkEntry(int upper, int lower) {
			this(upper >>> 8, upper & BYTE_MASK, lower);
		}

		int toUpper() {
			if (sectors < 0) {
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
