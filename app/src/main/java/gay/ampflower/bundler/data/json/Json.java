package gay.ampflower.bundler.data.json;

/**
 * @author Ampflower
 * @since ${version}
 **/
public sealed interface Json<T> permits JsonArray, JsonBoolean, JsonNull, JsonNumber, JsonObject, JsonString {
	default boolean asBoolean() {
		return asInt() != 0;
	}

	default byte asByte() {
		return (byte) asInt();
	}

	default short asShort() {
		return (short) asInt();
	}

	default int asInt() {
		throw new UnsupportedOperationException();
	}

	default long asLong() {
		throw new UnsupportedOperationException();
	}

	default float asFloat() {
		throw new UnsupportedOperationException();
	}

	default double asDouble() {
		throw new UnsupportedOperationException();
	}

	default String asString() {
		throw new UnsupportedOperationException();
	}

	default JsonArray asList() {
		return (JsonArray) this;
	}

	default JsonObject asCompound() {
		return (JsonObject) this;
	}

	default String asStringifiedJson() {
		return asStringifiedJson(new StringBuilder()).toString();
	}

	StringBuilder asStringifiedJson(StringBuilder builder);

	default void push(String field, Json<?> value) {
		throw new UnsupportedOperationException();
	}
}
