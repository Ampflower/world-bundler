package gay.ampflower.bundler.utils;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class Mint {
	public static long addClamp(final long a, final long b) {
		final long result = a + b;

		if (((a ^ result) & (b ^ result)) < 0) {
			return (~a >> 63) + Long.MIN_VALUE;
		}

		return result;
	}

	public static int addClamp(final int a, final int b) {
		final int result = a + b;

		if (((a ^ result) & (b ^ result)) < 0) {
			return (~a >> 31) + Integer.MIN_VALUE;
		}

		return result;
	}

	public static int clampAsInt(final long value) {
		if (value > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		if (value < Integer.MIN_VALUE) {
			return Integer.MIN_VALUE;
		}
		return (int) value;
	}
}
