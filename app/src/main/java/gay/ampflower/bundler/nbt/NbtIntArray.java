package gay.ampflower.bundler.nbt;

import java.util.Arrays;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class NbtIntArray implements Nbt<int[]> {
	private static final int[] SENTINEL = {};

	private int[] backing;

	public NbtIntArray() {
		this.backing = SENTINEL;
	}

	public NbtIntArray(int size) {
		this.backing = new int[size];
	}

	public NbtIntArray(int[] ints) {
		this.backing = ints;
	}

	@Override
	public int[] asIntsRaw() {
		return this.backing;
	}

	public void asInts(int[] ints) {
		this.backing = ints;
	}

	@Override
	public NbtType getType() {
		return NbtType.IntArray;
	}

	@Override
	public StringBuilder asStringifiedNbt(final StringBuilder builder) {
		return NbtUtil.truncWith(NbtUtil.toString(builder.append("[I;"), this.backing), ']');
	}

	@Override
	public String toString() {
		return "NbtIntArray" + Arrays.toString(this.backing);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof NbtIntArray other)) {
			return false;
		}
		return Arrays.equals(backing, other.backing);
	}
}
