package gay.ampflower.bundler.data.json.io;

import java.io.IOException;
import java.math.BigDecimal;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class FilteredJsonParser implements SaxJsonParser {
	protected final SaxJsonParser filtered;

	protected FilteredJsonParser(final SaxJsonParser filtered) {
		this.filtered = filtered;
	}

	@Override
	public void field(final String name) throws IOException {
		this.filtered.field(name);
	}

	@Override
	public void startList() throws IOException {
		this.filtered.startList();
	}

	@Override
	public void startCompound() throws IOException {
		this.filtered.startCompound();
	}

	@Override
	public void endTag() throws IOException {
		this.filtered.endTag();
	}

	@Override
	public void ofNull() throws IOException {
		this.filtered.ofNull();
	}

	@Override
	public void ofBoolean(final boolean value) throws IOException {
		this.filtered.ofBoolean(value);
	}

	@Override
	public void ofNumber(final BigDecimal value) throws IOException {
		this.filtered.ofNumber(value);
	}

	@Override
	public void ofString(final String value) throws IOException {
		this.filtered.ofString(value);
	}
}
