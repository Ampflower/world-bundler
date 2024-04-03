package gay.ampflower.bundler.nbt;

/**
 * @author Ampflower
 * @since ${version}
 **/
public sealed interface Nbt<T> permits NbtArrayList, NbtByteArray, NbtCompound, NbtIntArray, NbtList, NbtLongArray, NbtNull, NbtNumber, NbtString {
	byte TRUE = 1;
	byte FALSE = 0;

	default int getTypeRaw() {
		return this.getType().type;
	}

	NbtType getType();

	default boolean asBoolean() {
		return asInt() != 0;
	}

	default byte asByte() {
		return (byte) asInt();
	}

	default short asShort() {
		return (short) asInt();
	}

	default int asInt() {
		throw new UnsupportedOperationException();
	}

	default long asLong() {
		throw new UnsupportedOperationException();
	}

	default float asFloat() {
		throw new UnsupportedOperationException();
	}

	default double asDouble() {
		throw new UnsupportedOperationException();
	}

	default NbtByteArray asBytes() {
		return (NbtByteArray) this;
	}

	default byte[] asBytesRaw() {
		throw new UnsupportedOperationException();
	}

	default String asString() {
		throw new UnsupportedOperationException();
	}

	//@SuppressWarnings("unchecked")
	default <T extends Nbt<?>> NbtList<T> asList() {
		return (NbtList<T>) this;
	}

	default NbtCompound asCompound() {
		return (NbtCompound) this;
	}

	default NbtIntArray asInts() {
		return (NbtIntArray) this;
	}

	default int[] asIntsRaw() {
		throw new UnsupportedOperationException();
	}

	default NbtLongArray asLongs() {
		return (NbtLongArray) this;
	}

	default long[] asLongsRaw() {
		throw new UnsupportedOperationException();
	}

	default String asStringifiedNbt() {
		return asStringifiedNbt(new StringBuilder()).toString();
	}

	StringBuilder asStringifiedNbt(StringBuilder builder);

	default void push(String key, Nbt<?> value) {
		throw new UnsupportedOperationException("Attempted to push " + key + " -> " + value + " to " + this);
	}

	boolean equals(Object other);
}
