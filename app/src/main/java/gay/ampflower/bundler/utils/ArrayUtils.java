package gay.ampflower.bundler.utils;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.function.ToIntFunction;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class ArrayUtils {
	public static final VarHandle SHORTS_BIG_ENDIAN = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.BIG_ENDIAN);
	public static final VarHandle CHARS_BIG_ENDIAN = MethodHandles.byteArrayViewVarHandle(char[].class, ByteOrder.BIG_ENDIAN);
	public static final VarHandle INTS_BIG_ENDIAN = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);
	public static final VarHandle LONGS_BIG_ENDIAN = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);
	public static final VarHandle FLOATS_BIG_ENDIAN = MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.BIG_ENDIAN);
	public static final VarHandle DOUBLES_BIG_ENDIAN = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.BIG_ENDIAN);

	public static final int BYTE_STRIDE = 1;
	public static final int SHORT_STRIDE = 2;
	public static final int INT_STRIDE = 4;
	public static final int LONG_STRIDE = 8;
	public static final int FLOAT_STRIDE = 4;
	public static final int DOUBLE_STRIDE = 8;

	public static final byte[] SENTINEL_BYTES = new byte[0];

	public static <T> T[] sortedCopyOfRange(T[] input, int from, int to, Comparator<T> comparator) {
		final var array = Arrays.copyOfRange(input, from, to);
		Arrays.sort(array, comparator);
		return array;
	}

	public static void copyBigEndianShorts(byte[] read, int roff, short[] write, int woff, int len) {
		for (int i = 0; i < len; i++) {
			write[woff + i] = (short) SHORTS_BIG_ENDIAN.get(read, roff + i * SHORT_STRIDE);
		}
	}

	public static void copy(byte[] read, int roff, int[] write, int woff, int len, ByteOrder order) {
		final var handle = MethodHandles.byteArrayViewVarHandle(int[].class, order);

		for (int i = 0; i < len; i++) {
			write[woff + i] = (int) handle.get(read, roff + i * INT_STRIDE);
		}
	}

	public static void copyBigEndianInts(byte[] read, int roff, int[] write, int woff, int len) {
		for (int i = 0; i < len; i++) {
			write[woff + i] = (int) INTS_BIG_ENDIAN.get(read, roff + i * INT_STRIDE);
		}
	}

	public static void copy(int[] read, int roff, byte[] write, int woff, int len, ByteOrder order) {
		final var handle = MethodHandles.byteArrayViewVarHandle(int[].class, order);

		for (int i = 0; i < len; i++) {
			handle.set(write, woff + i * INT_STRIDE, read[roff + i]);
		}
	}

	public static void copyBigEndianInts(byte[] from, int[] to) {
		for (int i = 0; i < to.length; i++) {
			to[i] = (int) INTS_BIG_ENDIAN.get(from, i << 2);
		}
	}

	public static void copyBigEndianLongs(byte[] read, int roff, long[] write, int woff, int len) {
		for (int i = 0; i < len; i++) {
			write[woff + i] = (long) LONGS_BIG_ENDIAN.get(read, roff + i * LONG_STRIDE);
		}
	}

	public static int sum(final int[] ints) {
		int value = 0;

		for (int i : ints) {
			value += i;
		}

		return value;
	}

	public static <T> int sum(final T[] objects, ToIntFunction<T> function) {
		int value = 0;

		for (T t : objects) {
			value += function.applyAsInt(t);
		}

		return value;
	}

	public static <T> int sumNullable(final T[] objects, ToIntFunction<T> function) {
		int value = 0;

		for (T t : objects) {
			if (t != null) {
				value += function.applyAsInt(t);
			}
		}

		return value;
	}

	public static int max(final int[] ints) {
		int max = ints[0];

		for (int i = 1; i < ints.length; i++) {
			max = Math.max(max, ints[i]);
		}

		return max;
	}

	public static <T> int maxToInt(final T[] objects, ToIntFunction<T> function) {
		int max = function.applyAsInt(objects[0]);

		for (int i = 1; i < objects.length; i++) {
			max = Math.max(max, function.applyAsInt(objects[i]));
		}

		return max;
	}

	public static <T> int maxToIntNullable(final T[] objects, ToIntFunction<T> function, int def) {
		int max = applyOrDefault(objects[0], function, def);

		for (int i = 1; i < objects.length; i++) {
			max = Math.max(max, applyOrDefault(objects[i], function, def));
		}

		return max;
	}

	private static <T> int applyOrDefault(T object, ToIntFunction<T> function, int def) {
		if (object == null) {
			return def;
		}
		return function.applyAsInt(object);
	}

	public static String toString(Object array) {
		if (array instanceof boolean[] booleans) {
			return Arrays.toString(booleans);
		}
		if (array instanceof byte[] bytes) {
			return HexFormat.of().formatHex(bytes);
		}
		if (array instanceof short[] shorts) {
			return Arrays.toString(shorts);
		}
		if (array instanceof char[] chars) {
			return Arrays.toString(chars);
		}
		if (array instanceof int[] ints) {
			return Arrays.toString(ints);
		}
		if (array instanceof long[] longs) {
			return Arrays.toString(longs);
		}
		if (array instanceof float[] floats) {
			return Arrays.toString(floats);
		}
		if (array instanceof double[] doubles) {
			return Arrays.toString(doubles);
		}
		if (array instanceof Object[] objects) {
			return Arrays.toString(objects);
		}
		return "invalid: " + array;
	}
}
