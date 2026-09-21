package gay.ampflower.bundler.data.json;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class JsonUtil {
	public static boolean isProbablyJson(final String string) {
		if (string.isBlank() || string.length() < 2) {
			return false;
		}
		final char last = string.charAt(string.length() - 1);
		return switch (string.charAt(0)) {
			case '[' -> last == ']';
			case '{' -> last == '}';
			case '"' -> last == '"';
			default -> false;
		};
	}
}
