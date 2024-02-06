package gay.ampflower.bundler;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class TestUtils {
	@SafeVarargs
	public static <T> T[] array(T... array) {
		return array;
	}

	@SafeVarargs
	public static <T> Stream<T> stream(T... array) {
		return Arrays.stream(array);
	}

	public static Iterator<Object[]> zipArrays(final Object[] objectsA, final Object[] objectsB) {
		return zipArrays(stream(objectsA), objectsB);
	}

	public static Iterator<Object[]> zipArrays(final Iterable<?> iterable, final Object[] objectsB) {
		return zipArrays(StreamSupport.stream(iterable.spliterator(), false), objectsB);
	}

	public static Iterator<Object[]> zipArrays(final Stream<?> stream, final Object[] objectsB) {
		return stream.flatMap(a -> stream(objectsB).map(b -> array(a, b))).iterator();
	}
}
