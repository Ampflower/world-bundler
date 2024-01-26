package gay.ampflower.bundler.nbt;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class NbtCompound implements Nbt<Map<String, Nbt<?>>> {
	private final Map<String, Nbt<?>> backing;

	public NbtCompound() {
		this.backing = new Object2ObjectOpenHashMap<>();
	}

	public NbtCompound(int expectedSize) {
		this.backing = new Object2ObjectOpenHashMap<>(expectedSize);
	}

	public boolean hasKey(String key) {
		return this.hasKey(key, null);
	}

	public boolean hasKey(String key, NbtType type) {
		final var value = backing.get(key);
		return value != null && (type == null || value.getType() == type);
	}

	public boolean getBoolean(String key) {
		return get(key).asBoolean();
	}

	public byte getByte(String key) {
		return get(key).asByte();
	}

	public short getShort(String key) {
		return get(key).asShort();
	}

	public int getInt(String key) {
		return get(key).asInt();
	}

	public long getLong(String key) {
		return get(key).asLong();
	}

	public float getFloat(String key) {
		return get(key).asFloat();
	}

	public double getDouble(String key) {
		return get(key).asDouble();
	}

	public NbtByteArray getBytes(String key) {
		return get(key).asBytes();
	}

	public String getString(String key) {
		return get(key).asString();
	}

	public <V extends Nbt<?>> NbtList<V> getList(String key) {
		return get(key).asList();
	}

	public NbtCompound getCompound(String key) {
		return get(key).asCompound();
	}

	public NbtIntArray getInts(String key) {
		return get(key).asInts();
	}

	public NbtLongArray getLongs(String key) {
		return get(key).asLongs();
	}

	public Nbt<?> get(final String key) {
		return backing.get(key);
	}

	public Nbt<?> putBoolean(String key, boolean value) {
		return put(key, NbtByte.of(value));
	}

	public Nbt<?> putByte(String key, byte value) {
		return put(key, new NbtByte(value));
	}

	public Nbt<?> putShort(String key, short value) {
		return put(key, new NbtShort(value));
	}

	public Nbt<?> putInt(String key, int value) {
		return put(key, new NbtInt(value));
	}

	public Nbt<?> putLong(String key, long value) {
		return put(key, new NbtLong(value));
	}

	public Nbt<?> putFloat(String key, float value) {
		return put(key, new NbtFloat(value));
	}

	public Nbt<?> putDouble(String key, double value) {
		return put(key, new NbtDouble(value));
	}

	public Nbt<?> putBytes(String key, NbtByteArray value) {
		return put(key, value);
	}

	public Nbt<?> putString(String key, String value) {
		return put(key, new NbtString(value));
	}

	public Nbt<?> putList(String key, NbtList<?> value) {
		return put(key, value);
	}

	public Nbt<?> putCompound(String key, NbtCompound value) {
		return put(key, value);
	}

	public Nbt<?> putInts(String key, NbtIntArray value) {
		return put(key, value);
	}

	public Nbt<?> putLongs(String key, NbtLongArray value) {
		return put(key, value);
	}

	public Nbt<?> put(final String key, final Nbt<?> value) {
		if (value == null || value == NbtNull.Null) {
			throw new IllegalArgumentException("Attempted to put null @ " + key);
		}
		return backing.put(key, value);
	}

	public boolean isEmpty() {
		return backing.isEmpty();
	}

	@Override
	public void push(final String key, final Nbt<?> value) {
		put(key, value);
	}

	@Override
	public NbtType getType() {
		return NbtType.Compound;
	}

	@Override
	public StringBuilder asStringifiedNbt(final StringBuilder builder) {
		if (backing.isEmpty()) {
			return builder.append("{}");
		}
		builder.append('{');
		for (final var entry : backing.entrySet()) {
			entry.getValue().asStringifiedNbt(builder.append(entry.getKey()).append(':')).append(',');
		}
		return NbtUtil.truncWith(builder, '}');
	}

	@Override
	public String toString() {
		return asStringifiedNbt(new StringBuilder("NbtCompound")).toString();
	}
}
