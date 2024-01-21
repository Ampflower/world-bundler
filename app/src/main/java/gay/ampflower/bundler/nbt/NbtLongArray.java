package gay.ampflower.bundler.nbt;

import java.util.Arrays;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class NbtLongArray implements Nbt<long[]> {
	private static final long[] SENTINEL = {};

	private long[] backing;

	public NbtLongArray() {
		this.backing = SENTINEL;
	}

	public NbtLongArray(int size) {
		this.backing = new long[size];
	}

	public NbtLongArray(long[] longs) {
		this.backing = longs;
	}

	@Override
	public long[] asLongsRaw() {
		return this.backing;
	}

	public void setLongs(long[] longs) {
		this.backing = longs;
	}

	@Override
	public NbtType getType() {
		return NbtType.LongArray;
	}

	@Override
	public StringBuilder asStringifiedNbt(final StringBuilder builder) {
		return NbtUtil.truncWith(NbtUtil.toString(builder.append("[L;"), this.backing), ']');
	}

	@Override
	public String toString() {
		return "[L;" + Arrays.toString(this.backing) + ']';
	}
}
