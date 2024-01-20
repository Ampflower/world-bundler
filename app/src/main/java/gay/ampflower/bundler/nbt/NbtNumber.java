package gay.ampflower.bundler.nbt;

/**
 * @author Ampflower
 * @since ${version}
 **/
public sealed interface NbtNumber<T extends Number> extends Nbt<T> permits NbtByte, NbtDouble, NbtFloat, NbtInt, NbtLong, NbtShort {
	@Override
	byte asByte();

	@Override
	short asShort();

	@Override
	int asInt();

	@Override
	long asLong();

	@Override
	float asFloat();

	@Override
	double asDouble();

	@Override
	String asString();
}
