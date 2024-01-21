package gay.ampflower.bundler.nbt;

/**
 * @author Ampflower
 * @since ${version}
 **/
public record NbtShort(short value) implements NbtNumber<Short> {

	@Override
	public NbtType getType() {
		return NbtType.Short;
	}

	@Override
	public byte asByte() {
		return (byte) this.value;
	}

	@Override
	public short asShort() {
		return this.value;
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
		return Short.toString(this.value);
	}

	@Override
	public StringBuilder asStringifiedNbt(final StringBuilder builder) {
		return builder.append(value).append('s');
	}

	@Override
	public String toString() {
		return asString() + 's';
	}
}
