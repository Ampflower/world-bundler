package gay.ampflower.bundler.data.json.io;

import gay.ampflower.bundler.utils.StringUtils;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.BitSet;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class JsonWriter implements SaxJsonParser, AutoCloseable {
	private boolean requireComma;
	private int bitIndex;
	private final BitSet bitLists = new BitSet();
	private final Writer writer;

	private static final int
		bitIndexMask = 0x7FFFFFFF,
		bitIndexList = 0x80000000;

	public JsonWriter(final Writer writer) {
		this.writer = writer;
	}

	private void ofComma() throws IOException {
		if (requireComma) {
			writer.append(',');
		}
	}

	@Override
	public void field(final String name) throws IOException {
		if (bitIndex < 0) {
			throw new IllegalStateException("in list");
		}
		ofComma();
		StringUtils.quotedJsonEscapedString(this.writer, name);
		writer.append(':');
		requireComma = false;
	}

	@Override
	public void startList() throws IOException {
		pushTag('[', true);
	}

	@Override
	public void startCompound() throws IOException {
		pushTag('{', false);
	}

	private void pushTag(char c, boolean isList) throws IOException {
		ofComma();
		writer.append(c);
		requireComma = false;

		final int newBitIndex = (bitIndex & bitIndexMask) + 1;
		this.bitIndex = newBitIndex | (isList ? bitIndexList : 0);
		this.bitLists.set(newBitIndex, isList);
	}

	@Override
	public void endTag() throws IOException {
		writer.append(bitIndex < 0 ? ']' : '}');
		requireComma = true;

		final int newBitIndex = (bitIndex & bitIndexMask) - 1;
		this.bitIndex = newBitIndex | (this.bitLists.get(newBitIndex) ? bitIndexList : 0);
	}

	@Override
	public void ofNull() throws IOException {
		ofComma();
		writer.append(null);
		requireComma = true;
	}

	@Override
	public void ofBoolean(final boolean value) throws IOException {
		ofComma();
		writer.append(String.valueOf(value));
		requireComma = true;
	}

	@Override
	public void ofNumber(final BigDecimal value) throws IOException {
		ofComma();
		writer.append(value.toString());
		requireComma = true;
	}

	@Override
	public void ofString(final String value) throws IOException {
		ofComma();
		StringUtils.quotedJsonEscapedString(this.writer, value);
		requireComma = true;
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}
}
