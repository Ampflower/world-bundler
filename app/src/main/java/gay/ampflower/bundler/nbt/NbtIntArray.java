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
	public String toString() {
		return "NbtIntArray" + Arrays.toString(this.backing);
	}
}
