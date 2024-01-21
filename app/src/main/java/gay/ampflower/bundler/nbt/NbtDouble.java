package gay.ampflower.bundler.nbt;

/**
 * @author Ampflower
 * @since ${version}
 **/
public record NbtDouble(double value) implements NbtNumber<Double> {

	@Override
	public NbtType getType() {
		return NbtType.Double;
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
		return (float) this.value;
	}

	@Override
	public double asDouble() {
		return this.value;
	}

	@Override
	public String asString() {
		return Double.toString(this.value);
	}

	@Override
	public StringBuilder asStringifiedNbt(final StringBuilder builder) {
		return builder.append(value).append('d');
	}

	@Override
	public String toString() {
		return asString() + 'd';
	}
}
