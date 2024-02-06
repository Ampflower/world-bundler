package gay.ampflower.bundler.compress;

import gay.ampflower.bundler.utils.ArrayUtils;
import gay.ampflower.bundler.utils.LogUtils;
import org.slf4j.Logger;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static gay.ampflower.bundler.TestUtils.zipArrays;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class CompressionTest {
	private static final Logger logger = LogUtils.logger();

	private static final int init = 8192;
	private static final byte[] patternA, patternB, pattern00, patternF0, patternCC, patternAA, patternFF;

	static {
		pattern00 = new byte[init];
		patternF0 = new byte[init];
		Arrays.fill(patternF0, (byte) 0xF0);
		patternCC = new byte[init];
		Arrays.fill(patternCC, (byte) 0xCC);
		patternAA = new byte[init];
		Arrays.fill(patternAA, (byte) 0xAA);
		patternFF = new byte[init];
		Arrays.fill(patternFF, (byte) 0xFF);

		patternA = new byte[init];
		for (int i = 0; i < init; i++) {
			patternA[i] = (byte) i;
		}

		patternB = new byte[init];
		for (int i = 0; i < init; i += 2) {
			patternB[i] = (byte) 0xFF;
		}
	}

	@Test(invocationCount = 16, threadPoolSize = 16)
	public void raceConditionTest() throws IOException {
		final var dd = new ExecutableCompressor("dd");
		final var pa = patterns();
		for (int i = 0; i < 256; i++) {
			compressorTest(dd, pa[i % pa.length]);
		}
	}

	@Test(dataProvider = "processesAndPatterns")
	public void processesTest(Compressor compressor, byte[] test) throws IOException {
		compressorTest(compressor, test);
	}

	@Test(dataProvider = "compressorsAndPatterns")
	public void compressorTest(Compressor compressor, byte[] test) throws IOException {
		final var deflatedA = compressor.deflate(test);
		final var deflatedB = deflateWithStream(compressor, test);

		if (!Arrays.equals(deflatedA, deflatedB)) {
			logger.warn("Compressor {} emitted two values: {}, {} ({}, {})", compressor,
				System.identityHashCode(deflatedA), System.identityHashCode(deflatedB), deflatedA, deflatedB);
		}

		assertEquals(compressor.inflate(deflatedA), test);
		assertEquals(compressor.inflate(deflatedB), test);

		assertEquals(inflateWithStream(compressor, deflatedA), test);
		assertEquals(inflateWithStream(compressor, deflatedB), test);

		assertTrue(compressor.compatible(deflatedB),
			"Incompatible: " + ArrayUtils.hexString(deflatedB, 0, deflatedB.length, 32));
		assertTrue(compressor.compatible(deflatedA),
			"Incompatible: " + ArrayUtils.hexString(deflatedA, 0, deflatedA.length, 32));
	}

	@Test
	public void zstdMagic() {
		assertEquals(ZstdCompressor.magic, 0xFD2FB528);
	}

	private static byte[] deflateWithStream(Compressor compressor, byte[] bytes) throws IOException {
		final var bout = new ByteArrayOutputStream(init);

		try (final var deflater = compressor.deflater(bout)) {
			deflater.write(bytes);
		}

		return bout.toByteArray();
	}

	private static byte[] inflateWithStream(Compressor compressor, byte[] bytes) throws IOException {
		try (final var inflater = compressor.inflater(new ByteArrayInputStream(bytes))) {
			return inflater.readAllBytes();
		}
	}

	@DataProvider
	public static byte[][] patterns() {
		return new byte[][]{ArrayUtils.SENTINEL_BYTES,
			patternA, patternB, pattern00, patternF0, patternCC, patternAA, patternFF};
	}

	@DataProvider
	public static Compressor[] fileCompressors() {
		return CompressorRegistry.vanilla.fileCompressors;
	}

	@DataProvider
	public static Iterator<Compressor> compressors() {
		return CompressorRegistry.vanilla.customCompressors.values().iterator();
	}

	@DataProvider
	public static Iterator<Object[]> compressorsAndPatterns() {
		return zipArrays(CompressorRegistry.vanilla.customCompressors.values(), patterns());
	}

	@DataProvider
	public static Iterator<Object[]> processesAndPatterns() {
		return zipArrays(
			List.of(
				new ExecutableCompressor("cat"),
				new ExecutableCompressor("dd"),
				new ExecutableCompressor("bzip2 -z", "bzip2 -d", ArrayUtils.SENTINEL_BYTES),
				new ExecutableCompressor("bzip3 -e", "bzip3 -d", ArrayUtils.SENTINEL_BYTES),
				new ExecutableCompressor("lzma -z", "lzma -d", ArrayUtils.SENTINEL_BYTES)
			),
			patterns()
		);
	}
}
