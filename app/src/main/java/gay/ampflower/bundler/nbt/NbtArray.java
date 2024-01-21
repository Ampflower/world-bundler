package gay.ampflower.bundler.nbt;

import gay.ampflower.bundler.utils.ArrayUtils;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class NbtArray<T extends Nbt<?>> implements Nbt<T[]> {
	// :3
	private final Object backing;
	private final NbtType type;
	private int i = 0;

	public static final NbtArray<NbtNull> EMPTY = new NbtArray<>(NbtType.Null, new Nbt[0]);

	public NbtArray(NbtType type, Object object) {
		this.backing = object;
		this.type = type;
	}

	public NbtArray(int initial, NbtType type) {
		this.backing = type.genArray.apply(initial);
		this.type = type;
	}

	public boolean getBoolean(int key) {
		return getByte(key) != 0;
	}

	public byte getByte(int key) {
		return ((byte[]) backing)[key];
	}

	public short getShort(int key) {
		return ((short[]) backing)[key];
	}

	public int getInt(int key) {
		return ((int[]) backing)[key];
	}

	public long getLong(int key) {
		return ((long[]) backing)[key];
	}

	public float getFloat(int key) {
		return ((float[]) backing)[key];
	}

	public double getDouble(int key) {
		return ((double[]) backing)[key];
	}

	public NbtByteArray getBytes(int key) {
		return asNbtArray()[key].asBytes();
	}

	public String getString(int key) {
		return ((String[]) backing)[key];
	}

	public <V extends Nbt<?>> NbtList<V> getList(int key) {
		return asNbtArray()[key].asList();
	}

	public NbtCompound getCompound(int key) {
		return asNbtArray()[key].asCompound();
	}

	public NbtIntArray getInts(int key) {
		return asNbtArray()[key].asInts();
	}

	public NbtLongArray getLongs(int key) {
		return asNbtArray()[key].asLongs();
	}

	private Nbt<?>[] asNbtArray() {
		return (Nbt<?>[]) backing;
	}

	@Override
	public NbtByteArray asBytes() {
		return new NbtByteArray(asBytesRaw());
	}

	@Override
	public byte[] asBytesRaw() {
		return (byte[]) backing;
	}

	@Override
	public <T extends Nbt<?>> NbtList<T> asList() {
		return Nbt.super.asList();
	}

	@Override
	public NbtIntArray asInts() {
		return new NbtIntArray(asIntsRaw());
	}

	@Override
	public int[] asIntsRaw() {
		return (int[]) backing;
	}

	@Override
	public NbtLongArray asLongs() {
		return new NbtLongArray(asLongsRaw());
	}

	@Override
	public long[] asLongsRaw() {
		return (long[]) backing;
	}

	private void check(NbtType type, Nbt<?> addition) {
		if (!this.type.equals(type)) {
			throw new IllegalArgumentException("Cannot add " + addition + ": " + type + " is not " + this.type);
		}
		// This shouldn't ever be reached but you never know
		if (addition.getType() != type) {
			throw new IllegalArgumentException(addition + " is not " + type);
		}

		switch (this.type) {
			case Byte -> ((byte[]) backing)[i] = addition.asByte();
			case Short -> ((short[]) backing)[i] = addition.asShort();
			case Int -> ((int[]) backing)[i] = addition.asInt();
			case Long -> ((long[]) backing)[i] = addition.asLong();
			case Float -> ((float[]) backing)[i] = addition.asFloat();
			case Double -> ((double[]) backing)[i] = addition.asDouble();
			case ByteArray, List, Compound, IntArray, LongArray -> asNbtArray()[i] = addition;
			case String -> ((String[]) backing)[i] = addition.asString();
		}
		i++;
	}

	public void push(final String key, final Nbt<?> value) {
		check(value.getType(), value);
	}

	public NbtType getComponentType() {
		return this.type;
	}

	@Override
	public NbtType getType() {
		return NbtType.List;
	}

	@Override
	public StringBuilder asStringifiedNbt(StringBuilder builder) {
		return NbtUtil.toString(builder.append('['), backing).append(']');
	}

	@Override
	public String toString() {
		return "NbtArray<" + this.type + ">" + ArrayUtils.toString(backing);
	}
}
