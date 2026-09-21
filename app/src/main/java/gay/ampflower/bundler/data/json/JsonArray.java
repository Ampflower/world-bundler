package gay.ampflower.bundler.data.json;

import gay.ampflower.bundler.data.common.ListProxy;
import gay.ampflower.bundler.nbt.NbtUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class JsonArray extends ListProxy<Json<?>> implements Json<List<Json<?>>> {
	public JsonArray() {
		super(new ArrayList<>());
	}

	public JsonArray(int size) {
		super(new ArrayList<>(size));
	}

	@Override
	public StringBuilder asStringifiedJson(final StringBuilder builder) {
		if (this.backing.isEmpty()) {
			return builder.append("[]");
		}
		builder.append('[');
		for (final var entry : backing) {
			entry.asStringifiedJson(builder).append(',');
		}
		return NbtUtil.truncWith(builder, ']');
	}

	@Override
	public void push(final String field, final Json<?> value) {
		this.backing.add(value);
	}
}
