package gay.ampflower.bundler.nbt;

import java.util.Iterator;
import java.util.List;

/**
 * @author Ampflower
 * @since ${version}
 **/
public abstract sealed class NbtList<T extends Nbt<?>> implements Nbt<List<T>> permits NbtByteList, NbtDoubleList, NbtFloatList, NbtGenericList, NbtIntList, NbtLongList, NbtShortList {

	public static <T extends Nbt<?>> NbtList<T> of(NbtType type, int size) {
		return (NbtList<T>) switch (type) {
			case Byte -> new NbtByteList(size);
			case Short -> new NbtShortList(size);
			case Int -> new NbtIntList(size);
			case Long -> new NbtLongList(size);
			case Float -> new NbtFloatList(size);
			case Double -> new NbtDoubleList(size);
			default -> new NbtGenericList<T>(type, size);//throw new IllegalArgumentException("invalid type: " + type);
		};
	}

	public static NbtList<NbtNull> empty() {
		return new NbtGenericList<>(NbtType.Null, 0);
	}

	public abstract T get(int key);

	public boolean getBoolean(int key) {
		return get(key).asBoolean();
	}

	public byte getByte(int key) {
		return get(key).asByte();
	}

	public short getShort(int key) {
		return get(key).asShort();
	}

	public int getInt(int key) {
		return get(key).asInt();
	}

	public long getLong(int key) {
		return get(key).asLong();
	}

	public float getFloat(int key) {
		return get(key).asFloat();
	}

	public double getDouble(int key) {
		return get(key).asDouble();
	}

	public NbtByteArray getBytes(int key) {
		return get(key).asBytes();
	}

	public String getString(int key) {
		return get(key).asString();
	}

	public <T extends Nbt<?>> NbtList<T> getList(int key) {
		return get(key).asList();
	}

	public NbtCompound getCompound(int key) {
		return get(key).asCompound();
	}

	public NbtIntArray getInts(int key) {
		return get(key).asInts();
	}

	public NbtLongArray getLongs(int key) {
		return get(key).asLongs();
	}

	public boolean add(boolean value) {
		return addChecked(NbtByte.of(value));
	}

	public boolean add(byte value) {
		return addChecked(new NbtByte(value));
	}

	public boolean add(short value) {
		return addChecked(new NbtShort(value));
	}

	public boolean add(int value) {
		return addChecked(new NbtInt(value));
	}

	public boolean add(long value) {
		return addChecked(new NbtLong(value));
	}

	public boolean add(float value) {
		return addChecked(new NbtFloat(value));
	}

	public boolean add(double value) {
		return addChecked(new NbtDouble(value));
	}

	public boolean add(NbtByteArray value) {
		return addChecked(value);
	}

	public boolean add(String value) {
		return addChecked(new NbtString(value));
	}

	public boolean add(NbtList<?> value) {
		return addChecked(value);
	}

	public boolean add(NbtCompound value) {
		return addChecked(value);
	}

	public boolean add(NbtIntArray value) {
		return addChecked(value);
	}

	public boolean add(NbtLongArray value) {
		return addChecked(value);
	}

	private void check(NbtType type, Object addition) {
		final NbtType self = this.getComponentType();
		if (!self.equals(type)) {
			throw new IllegalArgumentException("Cannot add " + addition + ": " + type + " is not " + self);
		}
	}

	@SuppressWarnings("unchecked")
	private boolean check(NbtType type, Nbt<?> addition) {
		// This shouldn't ever be reached but you never know
		if (addition.getType() != type) {
			throw new IllegalArgumentException(addition + " is not " + type);
		}
		this.check(type, (Object) addition);
		return this.add((T) addition);
	}

	public abstract boolean add(T nbt);

	public boolean addChecked(Nbt<?> nbt) {
		return check(nbt.getType(), nbt);
	}

	public void push(final String key, final Nbt<?> value) {
		addChecked(value);
	}

	public abstract NbtType getComponentType();

	public abstract boolean remove(Nbt<?> nbt);

	public abstract int size();

	public boolean isEmpty() {
		return this.size() == 0;
	}

	@Override
	public final NbtType getType() {
		return NbtType.List;
	}

	@Override
	public StringBuilder asStringifiedNbt(final StringBuilder builder) {
		if (this.isEmpty()) {
			return builder.append("[]");
		}
		builder.append('[');
		for (final var value : (Iterable<T>) this::nbtIterator) {
			value.asStringifiedNbt(builder).append(',');
		}
		return NbtUtil.truncWith(builder, ']');
	}

	@Override
	public abstract String toString();

	@Override
	public abstract boolean equals(final Object obj);

	public abstract Iterator<T> nbtIterator();
}
