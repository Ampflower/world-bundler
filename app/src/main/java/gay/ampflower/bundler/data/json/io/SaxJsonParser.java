package gay.ampflower.bundler.data.json.io;

import gay.ampflower.bundler.data.json.Json;
import gay.ampflower.bundler.data.json.JsonArray;
import gay.ampflower.bundler.data.json.JsonBoolean;
import gay.ampflower.bundler.data.json.JsonNull;
import gay.ampflower.bundler.data.json.JsonNumber;
import gay.ampflower.bundler.data.json.JsonObject;
import gay.ampflower.bundler.data.json.JsonString;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * @author Ampflower
 * @since ${version}
 **/
public interface SaxJsonParser {
	void field(final String name) throws IOException;

	void startList() throws IOException;

	void startCompound() throws IOException;

	void endTag() throws IOException;

	void ofNull() throws IOException;

	void ofBoolean(boolean value) throws IOException;

	// note: this cannot be processed AOT due to the infinite length possibility that can be load bearing.
	// We do not know if it is load bearing, we must just assume it is.
	void ofNumber(BigDecimal value) throws IOException;

	void ofString(String value) throws IOException;

	default void push(Json<?> value) throws IOException {
		switch (value) {
			case JsonNull nill -> ofNull();
			case JsonBoolean bool -> ofBoolean(bool.asBoolean());
			case JsonNumber number -> ofNumber(number.value());
			case JsonString string -> ofString(string.value());
			case JsonArray array -> push(array);
			case JsonObject object -> push(object);
		}
	}

	default void push(JsonArray value) throws IOException {
		startList();
		for (final var entry : value) {
			push(entry);
		}
		endTag();
	}

	default void push(JsonObject value) throws IOException {
		startCompound();
		for (final var entry : value.entries()) {
			field(entry.getKey());
			push(entry.getValue());
		}
		endTag();
	}
}
