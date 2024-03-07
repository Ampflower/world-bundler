package gay.ampflower.bundler.nbt;

import gay.ampflower.bundler.nbt.io.NbtReader;
import gay.ampflower.bundler.nbt.io.NbtWriter;
import gay.ampflower.bundler.nbt.io.SaxNbtReader;
import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.utils.io.IoUtils;
import org.slf4j.Logger;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertEquals;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class NbtTest {
	private static final Logger logger = LogUtils.logger();
	private static final boolean dump = Boolean.getBoolean("bundler.test.nbt.dump");

	@Test
	public void bigtest() throws IOException {
		final byte[] original;
		try (final var bigtest = NbtTest.class.getResourceAsStream("/data/bundler/test/nbt/modern-bigtest.nbt")) {
			original = bigtest.readAllBytes();
		}

		final byte[] outputArray;
		try (
			final var byteOutput = new ByteArrayOutputStream(original.length);
			final var nbtWriter = new NbtWriter(byteOutput);
			final var byteInput = new ByteArrayInputStream(original);
			final var nbtReader = new NbtReader(byteInput);
		) {
			SaxNbtReader.parse(nbtWriter, nbtReader);

			nbtReader.close();
			nbtWriter.close();

			outputArray = byteOutput.toByteArray();
		}

		if (dump) {
			final var output = Files.createTempFile(Path.of("."), "dump-modern-bigtest-", ".dat");
			try (final var tmp = Files.newOutputStream(output)) {
				tmp.write(outputArray);
			}

			logger.info("sample @ {}", output);
		}


		logger.info("Verifying {}", original);
		final var a = IoUtils.verifyNbt(original, -2);

		logger.info("Verifying {}", outputArray);
		final var b = IoUtils.verifyNbt(outputArray, -1);

		assertEquals(b, a);

		assertEquals(outputArray, original);
	}
}
