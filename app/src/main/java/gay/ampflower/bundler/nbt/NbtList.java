package gay.ampflower.bundler.nbt;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class NbtList<T extends Nbt<?>> implements Nbt<List<T>> {
	private final List<T> backing;
	private NbtType type;

	public NbtList() {
		this.backing = new ArrayList<>();
	}

	public NbtList(int initial) {
		this.backing = new ArrayList<>(initial);
	}

	public NbtList(NbtType type) {
		this();
		this.type = type;
	}

	public NbtList(int initial, NbtType type) {
		this(initial);
		this.type = type;
	}

	public boolean getBoolean(int key) {
		return backing.get(key).asBoolean();
	}

	public byte getByte(int key) {
		return backing.get(key).asByte();
	}

	public short getShort(int key) {
		return backing.get(key).asShort();
	}

	public int getInt(int key) {
		return backing.get(key).asInt();
	}

	public long getLong(int key) {
		return backing.get(key).asLong();
	}

	public float getFloat(int key) {
		return backing.get(key).asFloat();
	}

	public double getDouble(int key) {
		return backing.get(key).asDouble();
	}

	public NbtByteArray getBytes(int key) {
		return backing.get(key).asBytes();
	}

	public String getString(int key) {
		return backing.get(key).asString();
	}

	public <T extends Nbt<?>> NbtList<T> getList(int key) {
		return backing.get(key).asList();
	}

	public NbtCompound getCompound(int key) {
		return backing.get(key).asCompound();
	}

	public NbtIntArray getInts(int key) {
		return backing.get(key).asInts();
	}

	public NbtLongArray getLongs(int key) {
		return backing.get(key).asLongs();
	}

	public boolean addBoolean(boolean value) {
		return addChecked(NbtByte.of(value));
	}

	public boolean addByte(byte value) {
		return addChecked(new NbtByte(value));
	}

	public boolean addShort(short value) {
		return addChecked(new NbtShort(value));
	}

	public boolean addInt(int value) {
		return addChecked(new NbtInt(value));
	}

	public boolean addLong(long value) {
		return addChecked(new NbtLong(value));
	}

	public boolean addFloat(float value) {
		return addChecked(new NbtFloat(value));
	}

	public boolean addDouble(double value) {
		return addChecked(new NbtDouble(value));
	}

	public boolean addBytes(NbtByteArray value) {
		return addChecked(value);
	}

	public boolean addString(String value) {
		return addChecked(new NbtString(value));
	}

	public boolean addList(NbtList<?> value) {
		return addChecked(value);
	}

	public boolean addCompound(NbtCompound value) {
		return addChecked(value);
	}

	public boolean addInts(NbtIntArray value) {
		return addChecked(value);
	}

	public boolean addLongs(NbtLongArray value) {
		return addChecked(value);
	}

	@SuppressWarnings("unchecked")
	private boolean check(NbtType type, Nbt<?> addition) {
		if (this.type != null && !this.type.equals(type)) {
			throw new IllegalArgumentException("Cannot add " + addition + ": " + type + " is not " + this.type);
		}
		// This shouldn't ever be reached but you never know
		if (addition.getType() != type) {
			throw new IllegalArgumentException(addition + " is not " + type);
		}
		this.type = type;
		return this.backing.add((T) addition);
	}

	public boolean add(T nbt) {
		return backing.add(nbt);
	}

	public boolean addChecked(Nbt<?> nbt) {
		return check(nbt.getType(), nbt);
	}

	public void push(final String key, final Nbt<?> value) {
		addChecked(value);
	}

	public NbtType getComponentType() {
		return this.type;
	}

	@Override
	public NbtType getType() {
		return NbtType.List;
	}

	@Override
	public StringBuilder asStringifiedNbt(final StringBuilder builder) {
		if (this.backing.isEmpty()) {
			return builder.append("[]");
		}
		builder.append('[');
		for (final var value : backing) {
			value.asStringifiedNbt(builder).append(',');
		}
		return NbtUtil.truncWith(builder, ']');
	}

	@Override
	public String toString() {
		return "NbtList<" + this.type + ">" + this.backing;
	}
}
