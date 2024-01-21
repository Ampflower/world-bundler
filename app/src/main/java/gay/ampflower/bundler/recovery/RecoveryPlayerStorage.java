package gay.ampflower.bundler.recovery;

import java.nio.file.Path;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class RecoveryPlayerStorage {
	private final Path root;
	private final Type type;

	public RecoveryPlayerStorage(Path root, Type type) {
		this.root = root;
		this.type = type;
	}

	public enum Type {
		Json, Nbt
	}
}
