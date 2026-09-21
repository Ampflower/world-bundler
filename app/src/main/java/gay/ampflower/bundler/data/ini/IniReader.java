package gay.ampflower.bundler.data.ini;

import java.io.IOException;

/**
 * @author Ampflower
 * @since ${version}
 **/
class IniReader implements IniStream {
	final Ini ini = new Ini();

	@Override
	public void ofEntry(final String section, final String key, final String value) throws IOException {
		ini.put(section, key, value);
	}
}
