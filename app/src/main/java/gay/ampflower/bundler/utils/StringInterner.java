package gay.ampflower.bundler.utils;

import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.Arrays;

public final class StringInterner {
	private static final String[] empty = new String[0];

	private final Char2ObjectOpenHashMap<Int2ObjectOpenHashMap<String[]>> backing = new Char2ObjectOpenHashMap<>();

	private Int2ObjectOpenHashMap<String[]> get(char c) {
		return backing.computeIfAbsent(c, $ -> new Int2ObjectOpenHashMap<>());
	}

	private static String find(final String[] haystack, final char[] needle, final int s, final int e) {
		for (final String value : haystack) {
			if (equals(value, needle, s, e)) return value;
		}
		return null;
	}

	private static String impl(final char[] buf, final int s, final int e) {
		return new String(buf, s, e - s).intern();
	}

	private static String put(final char[] buf, final int s, final int e,
							  final Int2ObjectOpenHashMap<String[]> map, final int hash, final String[] values) {
		final String intern = impl(buf, s, e);

		final int i = values.length;
		final String[] replacement = Arrays.copyOf(values, i + 1);
		replacement[i] = intern;

		map.put(hash, replacement);

		return intern;
	}

	public String substring(final char[] buf, final int s, final int e) {
		if (s == e) {
			return "";
		}

		final var map = get(buf[s]);

		final int hash = hash(buf, s, e);

		final String[] values = map.get(hash);

		if (values == null) {
			return put(buf, s, e, map, hash, empty);
		}

		final String value = find(values, buf, s, e);

		if (value == null) {
			return put(buf, s, e, map, hash, values);
		}

		return value;
	}

	public static boolean equals(final String str, final char[] buf, int s, final int e) {
		if (str.length() != e - s) {
			return false;
		}
		for (int i = 0; s < e; s++, i++) {
			if (str.charAt(i) != buf[s]) return false;
		}
		return true;
	}

	public static int hash(final char[] buf, int s, final int e) {
		int h = 0;
		for (; s < e; s++) h = h * 31 + buf[s];
		return h;
	}
}
