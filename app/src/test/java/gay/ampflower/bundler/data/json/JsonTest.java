package gay.ampflower.bundler.data.json;

import gay.ampflower.bundler.data.json.io.JsonReader;
import gay.ampflower.bundler.data.json.io.JsonWriter;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import static org.testng.Assert.assertEquals;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class JsonTest {
	@Test
	public void nano() throws IOException {
		final var sample = """
			{"stats":{},"DataVersion":3955}""";

		final var output = new StringWriter();

		try (
			final var writer = new JsonWriter(output);
			final var reader = new JsonReader(new StringReader(sample))
		) {
			reader.parse(writer);
		}

		assertEquals(output.toString(), sample);
	}
}
