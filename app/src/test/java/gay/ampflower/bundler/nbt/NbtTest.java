package gay.ampflower.bundler.nbt;

import gay.ampflower.bundler.nbt.io.NbtReader;
import gay.ampflower.bundler.nbt.io.NbtWriter;
import gay.ampflower.bundler.nbt.io.SaxNbtReader;
import gay.ampflower.bundler.nbt.io.SaxTreeWriter;
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

	@Test
	public void nbtCompoundToTree() throws IOException {
		final var writer = new SaxTreeWriter();
		writer.push(sample());

		assertEquals(writer.getRoot(), sample());
	}

	@Test
	public void nbtCompoundWriteRead() throws IOException {
		final var output = new ByteArrayOutputStream();
		try (output; final var writer = new NbtWriter(output)) {
			writer.push(sample());
		}

		if (dump) {
			final var tmpPath = Files.createTempFile(Path.of("."), "dump-nbtCompoundWriteRead-", ".dat");
			try (final var tmp = Files.newOutputStream(tmpPath)) {
				tmp.write(output.toByteArray());
			}

			logger.info("sample @ {}", tmpPath);
		}

		IoUtils.verifyNbt(output.toByteArray(), -1);

		final NbtCompound compound;
		try (
			final var input = new ByteArrayInputStream(output.toByteArray());
			final var reader = new NbtReader(input);
		) {
			final var writer = new SaxTreeWriter();
			SaxNbtReader.parse(writer, reader);
			compound = writer.getRoot().asCompound();
		}

		assertEquals(compound, sample());
	}

	/**
	 * Constructs an entirely new sample compound per call.
	 */
	private static NbtCompound sample() {
		final var compound = new NbtCompound();
		compound.putByte("byte", Byte.MAX_VALUE);
		compound.putShort("short", Short.MAX_VALUE);
		compound.putInt("int", Integer.MAX_VALUE);
		compound.putLong("long", Long.MAX_VALUE);
		compound.putFloat("float", Float.MAX_VALUE);
		compound.putDouble("double", Double.MAX_VALUE);
		compound.putString("string", "You should be able to parse a \u0000 easily, right?");
		compound.putBytes("bytes", new NbtByteArray(new byte[]{1, 1, 2, 3, 5}));
		compound.putInts("ints", new NbtIntArray(new int[]{1, 1, 2, 3, 5}));
		compound.putLongs("longs", new NbtLongArray(new long[]{1, 1, 2, 3, 5}));

		final var nest = new NbtCompound();
		nest.putList("bytes", new NbtByteList(new byte[]{5, 3, 2, 1, 1}));
		nest.putList("shorts", new NbtShortList(new short[]{5, 3, 2, 1, 1}));
		nest.putList("ints", new NbtIntList(new int[]{5, 3, 2, 1, 1}));
		nest.putList("longs", new NbtLongList(new long[]{5, 3, 2, 1, 1}));
		nest.putList("floats", new NbtFloatList(new float[]{5, 3, 2, 1, 1}));
		nest.putList("doubles", new NbtDoubleList(new double[]{5, 3, 2, 1, 1}));
		compound.putCompound("compound", nest);

		final var list = NbtList.of(NbtType.Compound, 1);
		list.add(new NbtCompound());
		compound.put("list", list);

		final var empty = NbtList.empty();
		compound.put("empty", empty);

		return compound;
	}
}
