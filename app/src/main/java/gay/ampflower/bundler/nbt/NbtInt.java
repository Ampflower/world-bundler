package gay.ampflower.bundler.nbt;

/**
 * @author Ampflower
 * @since ${version}
 **/
public record NbtInt(int value) implements NbtNumber<Integer> {

	@Override
	public NbtType getType() {
		return NbtType.Int;
	}

	@Override
	public byte asByte() {
		return (byte) this.value;
	}

	@Override
	public short asShort() {
		return (short) this.value;
	}

	@Override
	public int asInt() {
		return this.value;
	}

	@Override
	public long asLong() {
		return this.value;
	}

	@Override
	public float asFloat() {
		return this.value;
	}

	@Override
	public double asDouble() {
		return this.value;
	}

	@Override
	public String asString() {
		return Integer.toString(this.value);
	}

	@Override
	public String toString() {
		return asString();
	}
}
