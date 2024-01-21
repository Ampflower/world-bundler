package gay.ampflower.bundler.nbt;

/**
 * @author Ampflower
 * @since ${version}
 **/
public enum NbtNull implements Nbt<Void> {
	Null;

	@Override
	public String asStringifiedNbt() {
		return "null";
	}

	@Override
	public StringBuilder asStringifiedNbt(final StringBuilder builder) {
		return builder.append((String) null);
	}

	@Override
	public NbtType getType() {
		return NbtType.Null;
	}
}
