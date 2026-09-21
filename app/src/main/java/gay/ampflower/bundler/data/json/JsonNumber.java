package gay.ampflower.bundler.data.json;

import java.math.BigDecimal;

/**
 * @author Ampflower
 * @since ${version}
 **/
public record JsonNumber(BigDecimal value) implements Json<Number> {

	@Override
	public boolean asBoolean() {
		// if non-zero & has a value that is non-zero when rounded towards 0
		return value.signum() != 0 && value.precision() > value.scale();
	}

	@Override
	public byte asByte() {
		return value.byteValue();
	}

	@Override
	public short asShort() {
		return value.shortValue();
	}

	@Override
	public int asInt() {
		return value.intValue();
	}

	@Override
	public long asLong() {
		return value.longValue();
	}

	@Override
	public float asFloat() {
		return value.floatValue();
	}

	@Override
	public double asDouble() {
		return value.doubleValue();
	}

	@Override
	public String asString() {
		return value.toString();
	}

	@Override
	public StringBuilder asStringifiedJson(final StringBuilder builder) {
		return builder.append(asString());
	}
}
