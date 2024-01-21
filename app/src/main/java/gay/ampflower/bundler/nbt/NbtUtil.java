package gay.ampflower.bundler.nbt;

import java.util.function.Function;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class NbtUtil {
	public static StringBuilder toString(StringBuilder builder, Object array) {
		if (array instanceof boolean[] booleans) {
			return toString(builder, booleans);
		}
		if (array instanceof byte[] bytes) {
			return toString(builder, bytes);
		}
		if (array instanceof short[] shorts) {
			return toString(builder, shorts);
		}
		if (array instanceof char[] chars) {
			return toString(builder, chars);
		}
		if (array instanceof int[] ints) {
			return toString(builder, ints);
		}
		if (array instanceof long[] longs) {
			return toString(builder, longs);
		}
		if (array instanceof float[] floats) {
			return toString(builder, floats);
		}
		if (array instanceof double[] doubles) {
			return toString(builder, doubles);
		}
		if (array instanceof Object[] objects) {
			return toString(builder, objects);
		}
		return builder.append("invalid: ").append(array);
	}

	public static StringBuilder toString(StringBuilder builder, boolean[] array) {
		for (final var value : array) {
			builder.append(value).append('b').append(',');
		}
		return builder;
	}

	public static StringBuilder toString(StringBuilder builder, byte[] array) {
		for (final var value : array) {
			builder.append(value).append('b').append(',');
		}
		return builder;
	}

	public static StringBuilder toString(StringBuilder builder, char[] array) {
		for (final var value : array) {
			builder.append((int) value).append('s').append(',');
		}
		return builder;
	}

	public static StringBuilder toString(StringBuilder builder, short[] array) {
		for (final var value : array) {
			builder.append(value).append('s').append(',');
		}
		return builder;
	}

	public static StringBuilder toString(StringBuilder builder, int[] array) {
		for (final var value : array) {
			builder.append(value).append(',');
		}
		return builder;
	}

	public static StringBuilder toString(StringBuilder builder, long[] array) {
		for (final var value : array) {
			builder.append(value).append('l').append(',');
		}
		return builder;
	}

	public static StringBuilder toString(StringBuilder builder, float[] array) {
		for (final var value : array) {
			builder.append(value).append('f').append(',');
		}
		return builder;
	}

	public static StringBuilder toString(StringBuilder builder, double[] array) {
		for (final var value : array) {
			builder.append(value).append('d').append(',');
		}
		return builder;
	}

	public static StringBuilder toString(StringBuilder builder, Object[] array) {
		for (final var value : array) {
			builder.append(value).append(',');
		}
		return builder;
	}

	public static StringBuilder toString(StringBuilder builder, Nbt<?>[] array) {
		for (final var value : array) {
			value.asStringifiedNbt(builder).append(',');
		}
		return builder;
	}

	public static <T> StringBuilder toString(StringBuilder builder, T[] array, Function<T, String> toString) {
		for (final var value : array) {
			builder.append(toString.apply(value)).append(',');
		}
		return builder;
	}

	public static StringBuilder truncBy(StringBuilder builder, int len) {
		builder.setLength(builder.length() - len);
		return builder;
	}

	public static StringBuilder truncWith(StringBuilder builder, char c) {
		builder.setCharAt(builder.length() - 1, c);
		return builder;
	}
}
