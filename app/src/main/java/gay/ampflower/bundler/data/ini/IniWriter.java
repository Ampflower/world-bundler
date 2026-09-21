package gay.ampflower.bundler.data.ini;

import java.io.IOException;
import java.io.Writer;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class IniWriter implements AutoCloseable {
	private final Writer writer;

	public IniWriter(Writer writer) {
		this.writer = writer;
	}

	public void section(String section) throws IOException {
		writer.append("\r\n[").append(section).append("]\r\n");
	}

	public void entry(String key, String value) throws IOException {
		int i = key.indexOf('=');
		if (i >= 0) {
			throw new IllegalArgumentException(
				"Invalid character for key `" + key + "` at " + i + "; Paired with " + value);
		}
		i = Math.max(value.indexOf('\r'), value.indexOf('\n'));
		if (i >= 0) {
			throw new IllegalArgumentException(
				"Invalid character for value `" + value + "` at " + i + "; Paired with " + key);
		}
		writer.append(key).append('=').append(value).append("\r\n");
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}
}
