package gay.ampflower.bundler.nbt;

/**
 * @author Ampflower
 * @since ${version}
 **/
public enum NbtNull implements Nbt<Void> {
	Null;

	@Override
	public NbtType getType() {
		return NbtType.Null;
	}
}
