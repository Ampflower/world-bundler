package gay.ampflower.bundler.world.region;

import gay.ampflower.bundler.nbt.Nbt;
import gay.ampflower.bundler.nbt.NbtCompound;
import gay.ampflower.bundler.utils.*;
import gay.ampflower.bundler.world.Chunk;
import gay.ampflower.bundler.world.PotentialChunk;
import gay.ampflower.bundler.world.Region;
import gay.ampflower.bundler.world.io.ChunkReader;
import gay.ampflower.bundler.world.io.RegionHandler;
import gay.ampflower.bundler.world.util.ChunkDataUtil;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class McRegionRecoveryHandler extends McRegionHandler implements RegionHandler {
	private static final Logger logger = LogUtils.logger();

	public static final McRegionRecoveryHandler INSTANCE = new McRegionRecoveryHandler();

	/**
	 * Used to sniff out possible misaligned chunks.
	 * <p>
	 * {@code 128} is used as that is the smallest known sector ever used in production.
	 * There have been historically {@code 520} byte sectors, although the likelihood of
	 * a modern Minecraft world being hosted off ancient IBM drives is next to none.
	 */
	private static final int SECTOR_SNIFF = 128;

	@Override
	public Region readRegion(final int x, final int y, final InputStream stream, final ChunkReader chunkReader) throws IOException {
		final var buffer = stream.readAllBytes();
		if (stream.available() > 0) {
			logger.warn("Region [{},{}] appears underread, got {} bytes waiting", x, y, stream.available());
		}

		logger.info("Region [{},{}] occupies {} of memory", x, y, SizeUtils.displaySize(buffer.length));
		final var region = super.readRegion(x, y, new ByteArrayInputStream(buffer), chunkReader);

		if (region.isFull()) {
			logger.info("Assuming [{},{}] is okay, as fully read", x, y);
			logger.debug("Region: {}", region);
			return region;
		}

		if (region.isEmpty()) {
			logger.info("Region [{},{}] looks grim, nothing read with first pass", x, y);
		}

		final var sparse = new short[(buffer.length >>> 12) + 1];
		final var buf = new byte[SECTOR];
		final FirstSectorEntry[] entries;
		{
			final int[] offsets = new int[Region.CHUNK_COUNT];
			ArrayUtils.copy(buf, 0, offsets, 0, Region.CHUNK_COUNT, ByteOrder.BIG_ENDIAN);
			entries = parseFirstSector(offsets);
		}
		final int sectors, sectorOffset;

		{
			final var sectorMeta = computeSectors(x, y, sparse, entries);
			sectors = sectorMeta.sectors();
			sectorOffset = sectorMeta.sectorOffset();
		}

		if (sectors > sparse.length) {
			logger.warn("Region [{},{}] has max sector ({}) exceeding total sectors ({})", x, y, sectors, sparse.length);
			logger.warn("The file may have been totally overwritten, with low chance of data recovery.");
		}

		final var chunks = new ArrayList<PotentialChunk>();
		int previous = region.popCount();

		for (int i = 0; i < buffer.length; i += SECTOR_SNIFF) {
			final int expectedSize = (int) INT_HANDLE.get(buffer, i);
			final int compressorByte = buffer[i + 4] & COMPRESSION_MASK_MIN;
			PotentialChunk chunk = tryInflate(buffer, i + 5, expectedSize, LevelCompressor.getMcRegionCompressor(compressorByte));

			if (add(region, chunks, chunk)) {
				continue;
			}

			System.arraycopy(buffer, i, buf, 0, 8);
			chunk = tryInflate(buffer, i, -1, LevelCompressor.getFileCompressor(buf));

			if (add(region, chunks, chunk)) {
				continue;
			}

			System.arraycopy(buffer, i + 5, buf, 0, 8);
			chunk = tryInflate(buffer, i + 5, -1, LevelCompressor.getFileCompressor(buf));

			if (add(region, chunks, chunk)) {
				continue;
			}

			logger.trace("Could not find any chunk data at [{},{}][{}], continuing onto next sector", x, y, i);
		}

		if (!chunks.isEmpty()) {
			logger.info("{} chunks of foreign format in [{},{}]", chunks.size(), x, y);
			logger.trace("Failed chunks: {}", chunks);
		}

		logger.info("Recovered {} chunks from [{},{}]; previously {}", region.popCount(), x, y, previous);

		return region.meta(chunks);
	}

	private static SectorMeta computeSectors(final int x, final int y, final short[] mask, FirstSectorEntry[] entries) {
		Arrays.fill(mask, (short) -1);
		int sectorOffset = INITIAL_SECTOR_OFFSET;
		int sectors = 0;

		final var set = new Int2IntOpenHashMap(Region.CHUNK_COUNT);
		set.defaultReturnValue(-1);

		for (int i = 0; i < Region.CHUNK_COUNT; i++) {
			final var entry = entries[i];
			if (entry == FirstSectorEntry.SENTINEL) {
				continue;
			}
			if (entry.offset() == 1) {
				logger.info("Legacy McRegion detected; [{},{}][{}] reads at offset 1, timestamps won't be saved.", x, y, i);
				sectorOffset = 1;
			}

			final int entryEnd = entry.offset() + entry.sectors();
			if (entryEnd > mask.length) {
				logger.warn("Corrupted chunk entry [{},{}][{}]: {} exceeds allotted sector count {}",
					x, y, i, entry, mask.length);
			}

			int ms = -1, me = -1, l = Math.min(entryEnd, mask.length);
			for (int s = entry.offset(); s < l; s++) {
				final var tmp = mask[s];
				if (me != tmp) {
					if (ms >= 0 && me >= 0) {
						logger.warn("Overlapping chunk entry [{},{}][{}]: {} overlaps {} for {}-{}",
							x, y, i, entry, me, ms, s - 1);
					}
					me = tmp;
					ms = s;
				}
			}
			if (ms >= 0 && me >= 0) {
				logger.warn("Overlapping chunk entry [{},{}][{}]: {} overlaps {} for {}-{}",
					x, y, i, entry, me, ms, l - 1);
			}
			final var witnessValue = set.put(entry.offset(), i);
			if (witnessValue >= 0) {
				logger.warn("Corrupted chunk entry [{},{}][{}]: {} directly overlaps {} ({})",
					x, y, i, entry, witnessValue, entries[witnessValue]);
			}
			sectors = Math.max(entryEnd, sectors);
		}

		return new SectorMeta(sectors, sectorOffset);
	}

	@Override
	protected byte[] readIntoMemory(final InputStream stream, final int len) throws IOException {
		return stream.readAllBytes();
	}

	@Override
	protected PotentialChunk readChunk(final int x, final int y, final int i, final int size, final int compressorId,
												  final ChunkReader chunkReader, final LevelCompressor compressor, final byte[] bytes,
												  final int offset) throws IOException {
		if (compressorId < 0) {
			final byte[] chunk = chunkReader.readChunk(i, compressor);
			if (size != 0 && chunk != null) {
				logger.warn("Corrupted chunk [{},{}][{}]; found size {} for external chunk", x, y, i, size);
			}
			final NbtCompound nbt = chunk != null ? verify(x, y, i, chunk) : null;
			return new PotentialChunk(chunk, nbt);
		}

		if (size == 0) {
			logger.warn("Corrupted chunk [{},{}][{}]; zero-size with compressor {}",
				x, y, i, compressorId & COMPRESSION_MASK_ALL);
			return null;
		}

		if (size < 0) {
			logger.warn("Corrupted chunk [{},{}][{}]; negative size with compressor {}",
				x, y, i, compressorId & COMPRESSION_MASK_ALL);
			return null;
		}

		if (offset + 5 + size > bytes.length) {
			logger.warn("Corrupted chunk [{},{}][{}]; over-read: off: {}, len: {}, total: {}",
				x, y, i, offset + 5, size, bytes.length);
			return null;
		}

		try {
			final byte[] chunk = compressor.inflate(bytes, offset + 5, size);
			final NbtCompound nbt = verify(x, y, i, chunk);
			return new PotentialChunk(chunk, nbt);
		} catch (IOException ioe) {
			logger.warn("Failed to inflate [{},{}][{}] @ {} with byte[{}]; total size: {}, dumping",
				x, y, i, offset + 5, size, bytes.length, ioe);
			return new PotentialChunk(Arrays.copyOfRange(bytes, offset + 5, size), null);
		}
	}

	@Nullable
	private static PotentialChunk tryInflate(final @Nonnull byte[] buffer, final int offset, final int expectedSize,
														  final @Nullable LevelCompressor compressor) {
		if (compressor == null) {
			return null;
		}
		logger.trace("Obtained compressor {} @ offset {}, attempting to decompress data...", compressor, offset);
		try {
			final byte[] data = compressor.inflate(buffer, offset, buffer.length - offset);

			if (data.length == 0) {
				logger.debug("No data to be had from {}", offset);
				return null;
			}

			if (expectedSize == data.length) {
				logger.trace("Recovered {} bytes; length matches McRegion header", data.length);
			} else {
				logger.trace("Bytes mismatch, expected {}, got {}", expectedSize, data.length);
			}

			final var nbt = IoUtils.verifyNbt(data, offset);

			logger.info("Successfully read seemingly valid data @ {}", offset);
			logger.trace("Associated data: {}", nbt);
			return new PotentialChunk(data, nbt);
		} catch (IOException | IndexOutOfBoundsException | NegativeArraySizeException | AssertionError error) {
			logger.trace("Failed to read data @ {} with compressor {}", offset, compressor, error);
		}
		return null;
	}

	private static NbtCompound verify(final int x, final int y, final int i, final byte[] chunk) {
		try {
			return IoUtils.verifyNbt(chunk, i);
		} catch (AssertionError error) {
			logger.warn("Corrupted NBT @ [{},{}][{}]", x, y, i, error);
		}
		return null;
	}

	private static boolean add(Region region, ArrayList<PotentialChunk> chunks, PotentialChunk chunk) {
		if (chunk == null) {
			return false;
		}

		final var pos = ChunkDataUtil.getPosition(chunk.parsed());
		if (pos == null) {
			chunks.add(chunk);
		} else {
			final int c = Region.getChunkIndex(pos.x(), pos.y());
			final var regionChunks = region.chunks();
			final var regionChunk = regionChunks[c];

			if (regionChunk == null) {
				regionChunks[c] = new Chunk(pos.x(), pos.y(), -1, chunk.bytes(), chunk.parsed());
				return true;
			}

			if (!Arrays.equals(regionChunk.array(), chunk.bytes())) {
				final var regionLastUpdate = ChunkDataUtil.getLastUpdate((Nbt<?>) regionChunk.meta());
				final var chunkLastUpdate = ChunkDataUtil.getLastUpdate((Nbt<?>) chunk.parsed());

				if (chunkLastUpdate > regionLastUpdate) {
					logger.debug("Favouring [{},{}][{},{}] as it is newer; recovered: {} vs. old: {}",
						region.x(), region.y(), pos.x(), pos.y(), chunkLastUpdate, regionLastUpdate);
					regionChunks[c] = new Chunk(pos.x(), pos.y(), -1, chunk.bytes(), chunk.parsed());
					return true;
				}

				logger.debug("Dropping [{},{}][{},{}]", region.x(), region.y(), pos.x(), pos.y());
				logger.trace("{} was favoured over {}", regionChunk.meta(), chunk.parsed());
			}
		}
		return true;
	}
}
