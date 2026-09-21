package gay.ampflower.bundler.data.json.io;

import gay.ampflower.bundler.data.json.Json;
import gay.ampflower.bundler.utils.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class JsonTreeWriter implements SaxJsonParser {
	private static final Logger logger = LogUtils.logger();

	private final Deque<Json<?>> elements = new ArrayDeque<>();
	private String field;
	private Json<?> current, root;

	@Override
	public void field(final String name) throws IOException {
		this.field = field;
	}

	@Override
	public void startList() throws IOException {

	}

	@Override
	public void startCompound() throws IOException {

	}

	@Override
	public void endTag() throws IOException {

	}

	@Override
	public void ofNull() throws IOException {

	}

	private void pushLast(Json<?> tag) {
		final var last = this.current;
		if (last != null) {
			elements.push(last);
			last.push(field, tag);
		} else {
			root = tag;
		}
		this.current = tag;
	}

	@Override
	public void ofBoolean(final boolean value) throws IOException {

	}

	@Override
	public void ofNumber(final BigDecimal value) throws IOException {

	}

	@Override
	public void ofString(final String value) throws IOException {

	}
}
