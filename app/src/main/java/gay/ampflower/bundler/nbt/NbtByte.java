package gay.ampflower.bundler.nbt;

/**
 * @author Ampflower
 * @since ${version}
 **/
public record NbtByte(byte value) implements NbtNumber<Byte> {
	public static final NbtByte
		TRUE = new NbtByte(Nbt.TRUE),
		FALSE = new NbtByte(Nbt.FALSE);

	public static NbtByte of(boolean value) {
		if (value) {
			return TRUE;
		}
		return FALSE;
	}

	@Override
	public NbtType getType() {
		return NbtType.Byte;
	}

	@Override
	public byte asByte() {
		return this.value;
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
		return Byte.toString(this.value);
	}

	@Override
	public String toString() {
		return asString() + 'b';
	}
}
