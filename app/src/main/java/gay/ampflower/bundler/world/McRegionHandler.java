package gay.ampflower.bundler.world;

import gay.ampflower.bundler.utils.IoUtils;
import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.world.io.ChunkWriter;
import gay.ampflower.bundler.world.io.RegionHandler;
import gay.ampflower.bundler.world.io.RegionReader;
import gay.ampflower.bundler.world.io.RegionWriter;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorOutputStream;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

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
	private static final int CHUNK_CUTOFF = SECTOR * 255;

	private static final byte COMPRESSION_NONE = 0x00;
	private static final byte COMPRESSION_GZIP = 0x01;
	private static final byte COMPRESSION_ZLIB = 0x02;

	private static final byte COMPRESSION_FLAG_EXTERN = -0x80;

	@Override
	public Region readRegion(final InputStream stream) throws IOException {
		return null;
	}

	@Override
	public void writeRegion(final OutputStream stream, final Region region) throws IOException {
		final var entries = new ChunkEntry[Region.CHUNK_COUNT];
		final var sizes = new int[Region.CHUNK_COUNT];
		// FIXME: Don't clobber the array
		final var chunks = region.chunks();
		final var deflater = new Deflater(Deflater.BEST_COMPRESSION);

		final var buf = new byte[4096];
		int offset = 2;

		for(int i = 0; i < Region.CHUNK_COUNT; i++) {
			final var chunk = chunks[i];
			if(chunk != null) {
				IoUtils.verifyNbt(chunk);
				deflater.setInput(chunk);
				deflater.finish();
				chunks[i] = new byte[sectors(chunks[i].length) * 4096];
				sizes[i] = deflater.deflate(chunks[i]);
				if(!deflater.finished() || sizes[i] > CHUNK_CUTOFF) {
					logger.warn("Discarded {} as oversized: {}", i, sizes[i]);
					sizes[i] = 0;
					chunks[i] = null;
				} else {
					try (final var inflater = new InflaterInputStream(new ByteArrayInputStream(chunks[i], 0, sizes[i]))) {
						IoUtils.verifyNbt(inflater.readAllBytes());
					}
				}
				deflater.reset();

			}

			int sectors = sectors(sizes[i]);
			if(sectors == 0) {
				assert(sizes[i] == 0);
				entries[i] = ChunkEntry.SENTINEL;
			} else {
				entries[i] = new ChunkEntry(offset, sectors, region.timestamps()[i]);
				logger.info("Preparing {} -> {}", offset, sectors);
				offset += sectors;
			}
		}

		for(int i = 0; i < Region.CHUNK_COUNT; i++) {
			INT_HANDLE.set(buf, i * 4, entries[i].toUpper());
		}
		stream.write(buf);

		for(int i = 0; i < Region.CHUNK_COUNT; i++) {
			INT_HANDLE.set(buf, i * 4, entries[i].toLower());
		}
		stream.write(buf);

		for(int i = 0; i < Region.CHUNK_COUNT; i++) {
			if(sizes[i] == 0) continue;
			// Apparently includes the compression bit.
			INT_HANDLE.set(buf, 0, sizes[i] + 1);
			buf[4] = COMPRESSION_ZLIB;

			IoUtils.writeSectors(stream, chunks[i], 0, buf, 5, sizes[i]);
			stream.flush();
		}
	}

	public int sectors(int i) {
		if(i == 0) {
			return 0;
		}
		// Accounts for header;
		i += 5;
		return (i >> 12) + ((i & 4095) != 0 ? 1 : 0);
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
			return (offset << 8) | sectors;
		}

		int toLower() {
			return timestamp;
		}

		private static final ChunkEntry SENTINEL = new ChunkEntry(0, 0, 0);
	}
}
