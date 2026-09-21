package gay.ampflower.bundler.utils;

import it.unimi.dsi.fastutil.chars.Char2CharMap;
import it.unimi.dsi.fastutil.chars.Char2CharOpenHashMap;

import java.io.IOException;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class StringUtils {
	private static final Char2CharMap unsafeCharMap = new Char2CharOpenHashMap();

	static {
		unsafeCharMap.put('\0', '\uffff');
		unsafeCharMap.put('"', '"');
		unsafeCharMap.put('\b', 'b');
		unsafeCharMap.put('\t', 't');
		unsafeCharMap.put('\n', 'n');
		unsafeCharMap.put('\f', 'f');
		unsafeCharMap.put('\r', 'r');
		unsafeCharMap.put('\\', '\\');
	}

	public static void quotedJsonEscapedString(final Appendable appendable, final String string) throws IOException {
		// optimised path
		if (appendable instanceof StringBuilder builder) {
			quotedJsonEscapedString(builder, string);
			return;
		}

		appendable.append('"');

		jsonEscapeString(appendable, string);

		appendable.append('"');
	}

	public static void jsonEscapeString(final Appendable appendable, final String string) throws IOException {
		if (appendable instanceof StringBuilder builder) {
			int start = builder.length();
			jsonEscapeString(builder.append(appendable), start, start + string.length());
			return;
		}

		int lastCommitted = 0;

		for (int i = 0; i < string.length(); i++) {
			final char c = string.charAt(i);

			if (c == 0) {
				if (i > lastCommitted) {
					appendable.append(string, lastCommitted, i);
				}
				appendable.append("\\u0000");
				lastCommitted = i + 1;
				continue;
			}

			final char r = unsafeCharMap.get(c);
			if (r != 0) {
				if (i > lastCommitted) {
					appendable.append(string, lastCommitted, i);
				}
				appendable.append('\\').append(r);
				lastCommitted = i + 1;
			}
		}

		if (string.length() > lastCommitted) {
			appendable.append(string, lastCommitted, string.length());
		}
	}

	public static int quotedJsonEscapedString(final StringBuilder builder, final String string) {
		if (string == null) {
			builder.append((String) null);
			return 4;
		}

		final int start = builder.length() + 1;
		builder.append('"').append(string).append('"');
		final int end = builder.length() - 1;
		return StringUtils.jsonEscapeString(builder, start, end);
	}


	public static String jsonEscapeString(final String string) {
		final StringBuilder builder = new StringBuilder(string);
		int grown = jsonEscapeString(builder);
		if (grown == 0) {
			return string;
		}
		return builder.toString();
	}

	public static int jsonEscapeString(final StringBuilder builder) {
		return jsonEscapeString(builder, 0, builder.length());
	}

	public static int jsonEscapeString(final StringBuilder builder, int start, final int end) {
		int stop = end;
		while (start < stop) {
			final char c = builder.charAt(start);

			if (c == 0) {
				builder.replace(start, start++, "\\u0000");
				start += 6;
				stop += 6;
				continue;
			}

			final char r = unsafeCharMap.get(c);
			if (r != 0) {
				builder.setCharAt(start, r);
				builder.insert(start, '\\');
				start += 2;
				stop++;
				continue;
			}

			start++;
		}

		return stop - end;
	}
}
