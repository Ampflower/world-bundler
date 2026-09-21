package gay.ampflower.bundler.data.json;

import gay.ampflower.bundler.utils.StringUtils;

/**
 * @author Ampflower
 * @since ${version}
 **/
public record JsonString(String value) implements Json<String> {

	@Override
	public String asString() {
		return this.value;
	}

	@Override
	public StringBuilder asStringifiedJson(final StringBuilder builder) {
		StringUtils.quotedJsonEscapedString(builder, this.value);
		return builder;
	}

	@Override
	public String toString() {
		return StringUtils.jsonEscapeString(this.value);
	}
}
