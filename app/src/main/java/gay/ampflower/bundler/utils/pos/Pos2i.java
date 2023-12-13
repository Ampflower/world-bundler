package gay.ampflower.bundler.utils.pos;

/**
 * @author Ampflower
 * @since ${version}
 **/
public record Pos2i(int x, int y) {
	public Pos2i(long l) {
		this(x(l), y(l));
	}

	public long toLong() {
		return (long) x << 32 | y;
	}

	public static int x(long pos) {
		return (int) (pos >>> 32);
	}

	public static int y(long pos) {
		return (int) pos;
	}
}
