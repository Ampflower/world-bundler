package gay.ampflower.bundler.nbt;

/**
 * @author Ampflower
 * @since ${version}
 **/
public record NbtString(String value) implements Nbt<String> {

	@Override
	public NbtType getType() {
		return NbtType.String;
	}

	@Override
	public boolean asBoolean() {
		if ("true".equalsIgnoreCase(value)) {
			return true;
		}
		if ("false".equalsIgnoreCase(value)) {
			return false;
		}
		return Nbt.super.asBoolean();
	}

	@Override
	public byte asByte() {
		return Byte.parseByte(this.value);
	}

	@Override
	public short asShort() {
		return Short.parseShort(this.value);
	}

	@Override
	public int asInt() {
		return Integer.parseInt(this.value);
	}

	@Override
	public long asLong() {
		return Long.parseLong(this.value);
	}

	@Override
	public float asFloat() {
		return Float.parseFloat(this.value);
	}

	@Override
	public double asDouble() {
		return Double.parseDouble(this.value);
	}

	@Override
	public String asString() {
		return this.value;
	}

	@Override
	public StringBuilder asStringifiedNbt(final StringBuilder builder) {
		final int start = builder.length() + 1;
		builder.append('"').append(value).append('"');
		int end = builder.length() - 1;
		for (int i = start; i < end; i++) {
			if (builder.charAt(i) == '"') {
				builder.insert(i, '\\');
				i++;
				end++;
			}
		}
		return builder;
	}

	@Override
	public String toString() {
		return '"' + this.value + '"';
	}
}
