package gay.ampflower.bundler.utils;

/**
 * @author Ampflower
 * @since ${version}
 **/
public record Identifier(String namespace, String path) {

	private Identifier(final String id, final int col) {
		this(id.substring(0, col), id.substring(col + 1));
	}

	public static Identifier ofMinecraft(final String id) {
		final int col = id.indexOf(':');
		if (col >= 0) {
			return new Identifier(id, col);
		}
		return new Identifier("minecraft", id);
	}

	public static Identifier ofBundler(final String id) {
		final int col = id.indexOf(':');
		if (col >= 0) {
			return new Identifier(id, col);
		}
		return new Identifier("bundler", id);
	}

	public static Identifier of(final String namespace, final String path) {
		if (namespace.indexOf(':') >= 0) {
			throw new IllegalArgumentException("invalid namespace: `" + namespace + '`');
		}
		return new Identifier(namespace, path);
	}

	public String toString() {
		return namespace + ':' + path;
	}
}
