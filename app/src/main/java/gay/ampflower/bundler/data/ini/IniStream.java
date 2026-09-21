package gay.ampflower.bundler.data.ini;

import java.io.IOException;

/**
 * @author Ampflower
 * @since ${version}
 **/
@FunctionalInterface
public interface IniStream {
	default boolean shouldReadSection(String section) {
		return true;
	}

	void ofEntry(String section, String key, String value) throws IOException;
}
