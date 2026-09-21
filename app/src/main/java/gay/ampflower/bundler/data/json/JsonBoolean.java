package gay.ampflower.bundler.data.json;

/**
 * @author Ampflower
 * @since ${version}
 **/
public enum JsonBoolean implements Json<Boolean> {
	False,
	True,
	;

	@Override
	public boolean asBoolean() {
		return this == True;
	}

	@Override
	public int asInt() {
		return asBoolean() ? 1 : 0;
	}

	@Override
	public long asLong() {
		return asBoolean() ? 1 : 0;
	}

	@Override
	public float asFloat() {
		return asBoolean() ? 1 : 0;
	}

	@Override
	public double asDouble() {
		return asBoolean() ? 1 : 0;
	}

	@Override
	public String asString() {
		return String.valueOf(asBoolean());
	}

	@Override
	public StringBuilder asStringifiedJson(final StringBuilder builder) {
		return builder.append(asBoolean());
	}

	@Override
	public String toString() {
		return asString();
	}
}
