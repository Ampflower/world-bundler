package gay.ampflower.bundler.nbt;

/**
 * @author Ampflower
 * @since ${version}
 **/
public record NbtLong(long value) implements NbtNumber<Long> {

	@Override
	public NbtType getType() {
		return NbtType.Long;
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
		return Long.toString(this.value);
	}

	@Override
	public StringBuilder asStringifiedNbt(final StringBuilder builder) {
		return builder.append(value).append('l');
	}

	@Override
	public String toString() {
		return asString() + 'l';
	}
}
