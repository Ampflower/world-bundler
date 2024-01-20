package gay.ampflower.bundler.nbt;

/**
 * @author Ampflower
 * @since ${version}
 **/
public record NbtFloat(float value) implements NbtNumber<Float> {

	@Override
	public NbtType getType() {
		return NbtType.Float;
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
		return (int) this.value;
	}

	@Override
	public long asLong() {
		return (long) this.value;
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
		return Float.toString(this.value);
	}

	@Override
	public String toString() {
		return asString() + 'f';
	}
}
