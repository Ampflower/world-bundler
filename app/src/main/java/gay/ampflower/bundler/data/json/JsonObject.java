package gay.ampflower.bundler.data.json;

import gay.ampflower.bundler.nbt.NbtUtil;
import gay.ampflower.bundler.utils.StringUtils;

import java.util.Map;
import java.util.Set;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class JsonObject implements Json<Map<String, Json<?>>> {
	private final Map<String, Json<?>> backing;

	public JsonObject(final Map<String, Json<?>> backing) {
		this.backing = backing;
	}

	@Override
	public StringBuilder asStringifiedJson(final StringBuilder builder) {
		if (this.backing.isEmpty()) {
			return builder.append("{}");
		}
		builder.append('{');
		for (final var entry : backing.entrySet()) {
			StringUtils.quotedJsonEscapedString(builder, entry.getKey());
			entry.getValue().asStringifiedJson(builder.append(':')).append(',');
		}
		return NbtUtil.truncWith(builder, '}');
	}

	public Set<Map.Entry<String, Json<?>>> entries() {
		return this.backing.entrySet();
	}

	@Override
	public void push(final String field, final Json<?> value) {
		this.backing.put(field, value);
	}
}
