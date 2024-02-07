package gay.ampflower.bundler.utils.io;

import gay.ampflower.bundler.utils.LogUtils;
import org.slf4j.Logger;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class IoUtilsTest {
	private final Logger logger = LogUtils.logger();

	@Test
	public void sector4B() throws IOException {
		final var baos = new ByteArrayOutputStream();
		final var buf = new byte[4096];
		buf[3] = 5;
		buf[4] = 3;
		final var nbt = new byte[]{0x0A, 0, 0, 0};
		final var expected = new byte[4096];

		expected[3] = 5;
		expected[4] = 3;
		expected[5] = 10;

		IoUtils.writeSectors(baos, nbt, 0, buf, 5, 4);

		final var actual = baos.toByteArray();

		logger.info("Diff:\nActual:   {}\nExpected: {}", Arrays.toString(actual), Arrays.toString(expected));

		Assert.assertEquals(actual, expected);
	}
}
