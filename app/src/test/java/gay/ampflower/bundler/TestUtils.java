package gay.ampflower.bundler;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class TestUtils {
	public static <T> T[] array(T... array) {
		return array;
	}

	public static <T> Stream<T> stream(T... array) {
		return Arrays.stream(array);
	}
}
