package gay.ampflower.bundler.world.region;

import gay.ampflower.bundler.nbt.NbtCompound;
import gay.ampflower.bundler.utils.ArrayUtils;
import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.world.Chunk;
import gay.ampflower.bundler.world.Region;
import gay.ampflower.bundler.world.io.RegionHandler;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HexFormat;
import java.util.Iterator;

import static gay.ampflower.bundler.TestUtils.zipArrays;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class RegionHandlerTest {
	private static final RegionHandler[] handlers = {BundlerHandler.INSTANCE, LinearHandler.INSTANCE,
		McRegionHandler.INSTANCE, McRegionRecoveryHandler.INSTANCE};
	private static final Logger logger = LogUtils.logger();
	private static final NbtCompound empty = new NbtCompound();

	@Test
	public void testHandlersAndEmptyRegionsProvider() {
		handlersAndEmptyRegions();
	}

	@Test
	public void testHandlersAndRegionsProvider() {
		handlersAndRegions();
	}

	@Test(dataProvider = "handlersAndEmptyRegions")
	public void checkHandlerReturnsNull(final RegionHandler handler, final Region region) throws IOException {
		harnessWR(handler, region, new Region(region.x(), region.y()));
	}

	@Test(dataProvider = "handlersAndRegions")
	public void checkHandlerReadWrite(final RegionHandler handler, final Region region) throws IOException {
		this.harnessWR(handler, region, region);
	}

	public void harnessWR(final RegionHandler handler, final Region region, final Region expected) throws IOException {
		final var baos = new ByteArrayOutputStream();

		handler.writeRegion(baos, region);

		logger.info("Handler {} emitted: {}", handler, HexFormat.of().formatHex(baos.toByteArray()));


		final var bais = new ByteArrayInputStream(baos.toByteArray());

		final var sample = handler.readRegion(0, 0, bais);

		Assert.assertEquals(sample, expected);
	}

	public static Chunk emptyChunk(int i) {
		return new Chunk(Region.getChunkX(0, i), Region.getChunkY(0, i), 0, null, 0);
	}

	public static Chunk emptyCompoundChunk(int i) {
		return new Chunk(Region.getChunkX(0, i), Region.getChunkY(0, i), 0, empty, 4);
	}

	private static Chunk[] emptyChunks() {
		final var chunks = new Chunk[Region.CHUNK_COUNT];
		for (int i = 0; i < Region.CHUNK_COUNT; i++) {
			if (chunks[i] == null) {
				chunks[i] = new Chunk(0, 0, i, 0, ArrayUtils.SENTINEL_BYTES);
			}
		}
		return chunks;
	}

	@DataProvider
	public static Region[] emptyRegions() {
		return new Region[]{
			new Region(0, 0, new Chunk[Region.CHUNK_COUNT]),
			new Region(0, 0, emptyChunks()),
		};
	}

	@DataProvider
	public static Region[] regions() {
		final var regions = new Region[1024 + 1024 + 11];

		for (int i = 0; i < Region.CHUNK_COUNT; i++) {
			final var chunks = new Chunk[Region.CHUNK_COUNT];
			chunks[i] = emptyCompoundChunk(i);
			regions[i] = new Region(0, 0, chunks);
		}

		final var chunksTemplate = new Chunk[Region.CHUNK_COUNT];
		for (int c = 0; c < Region.CHUNK_COUNT; c++) {
			chunksTemplate[c] = emptyChunk(c);
		}

		for (int i = 0; i < Region.CHUNK_COUNT; i++) {
			final var chunks = chunksTemplate.clone();
			chunks[i] = emptyCompoundChunk(i);
			regions[i + 1024] = new Region(0, 0, chunks);
		}

		for (int i = 0; i < 11; i++) {
			final var chunks = new Chunk[Region.CHUNK_COUNT];
			int jmp = 1 << i, masq = jmp - 1;
			for (int c = 0; c < Region.CHUNK_COUNT; c++) {
				chunks[c] = emptyCompoundChunk(c);
				if ((c & masq) == masq) c += jmp;
			}
			regions[i + 2048] = new Region(0, 0, chunks);
		}

		return regions;
	}

	@DataProvider
	public static RegionHandler[] handlers() {
		return handlers;
	}

	@DataProvider
	public static Iterator<Object[]> handlersAndEmptyRegions() {
		return zipArrays(handlers, emptyRegions());
	}

	@DataProvider
	public static Iterator<Object[]> handlersAndRegions() {
		return zipArrays(handlers, regions());
	}
}
