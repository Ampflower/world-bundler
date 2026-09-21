package gay.ampflower.bundler.data.json;

/**
 * @author Ampflower
 * @since ${version}
 **/
public enum JsonNull implements Json<Void> {
	Null;

	@Override
	public String toString() {
		return "null";
	}

	@Override
	public StringBuilder asStringifiedJson(final StringBuilder builder) {
		return builder.append((String) null);
	}
}
