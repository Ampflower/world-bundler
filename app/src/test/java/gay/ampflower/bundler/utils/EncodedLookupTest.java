package gay.ampflower.bundler.utils;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import org.slf4j.Logger;
import org.testng.annotations.Test;

import java.lang.foreign.MemorySegment;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class EncodedLookupTest {
	private final Logger logger = LogUtils.logger();

	@Test
	public void terse() {
		final var map = new EncodedStringMap(
			List.of(StandardCharsets.UTF_8, StandardCharsets.UTF_16LE, StandardCharsets.UTF_16BE),
			List.of(
				"this",
				"gay",
				"lesbian",
				"is",
				"unapologetically",
				"queer",
				"and",
				"is",
				"a",
				"cat",
				"nya",
				"meow",
				"mrrp"
			)
		);

		final String sample = """
			Hello there queers, today we're doing a lesson in becoming a cat.
			First, to become a gay kitty, we must learn how to meow. *nya~*
			Then, to be a lesbian while unapologetically meowing,
			you shall mrrp until this sample is satisfied.

			Until then, meow and don't forget to purr.""";

		logger.info("Passing sample `{}` to the lookup decoder, {}", sample, map);

		// All samples here shall match.
		final byte[] latin = sample.getBytes(StandardCharsets.ISO_8859_1);
		final byte[] ascii = sample.getBytes(StandardCharsets.US_ASCII);
		final byte[] utf8 = sample.getBytes(StandardCharsets.UTF_8);
		final byte[] utf16 = sample.getBytes(StandardCharsets.UTF_16);
		final byte[] utf16b = sample.getBytes(StandardCharsets.UTF_16BE);
		final byte[] utf16l = sample.getBytes(StandardCharsets.UTF_16LE);

		assertValidity("latin", latin, map);
		assertValidity("ascii", ascii, map);
		assertValidity("utf8", utf8, map);
		assertValidity("utf16", utf16, map);
		assertValidity("utf16b", utf16b, map);
		assertValidity("utf16l", utf16l, map);
	}

	private void assertValidity(
		final String charset,
		final byte[] bytes,
		final EncodedStringMap map
	) {
		logger.info("{} as byte[]", charset);
		var a = assertResult(map.scan(bytes));
		logger.info("{} as MemorySegment", charset);
		var b = assertResult(map.scan(MemorySegment.ofArray(bytes)));

		assertEquals(a, b);
	}

	private Long2ObjectMap<EncodedStringMap.Result> assertResult(Long2ObjectMap<EncodedStringMap.Result> results) {
		assertFalse(results.isEmpty(), "No results found within map");

		final var itr = Long2ObjectMaps.fastIterator(results);
		while (itr.hasNext()) {
			final var entry = itr.next();

			final var result = entry.getValue();
			logger.info(
				"Found at offset {}: `{}` encoded with `{}`",
				entry.getLongKey(),
				result.value(),
				result.charset()
			);
		}

		return results;
	}
}
