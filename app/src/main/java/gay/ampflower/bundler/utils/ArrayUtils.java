package gay.ampflower.bundler.utils;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Comparator;

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

	public static final int SHORT_STRIDE = 2;
	public static final int INT_STRIDE = 4;
	public static final int LONG_STRIDE = 8;

	public static <T> T[] sortedCopyOfRange(T[] input, int from, int to, Comparator<T> comparator) {
		final var array = Arrays.copyOfRange(input, from, to);
		Arrays.sort(array, comparator);
		return array;
	}

	public static void copy(byte[] read, int roff, int[] write, int woff, int len, ByteOrder order) {
		final var handle = MethodHandles.byteArrayViewVarHandle(int[].class, order);

		for(int i = 0; i < len; i++) {
			write[woff + i] = (int)handle.get(read, roff + i * INT_STRIDE);
		}
	}

	public static void copyBigEndianInts(byte[] from, int[] to) {
		for(int i = 0; i < to.length; i++) {
			to[i] = (int) INTS_BIG_ENDIAN.get(from, i << 2);
		}
	}
}
