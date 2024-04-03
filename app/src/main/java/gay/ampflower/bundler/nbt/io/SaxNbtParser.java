package gay.ampflower.bundler.nbt.io;

import gay.ampflower.bundler.nbt.*;

import java.io.IOException;

/**
 * @author Ampflower
 * @since ${version}
 **/
public interface SaxNbtParser {
	void field(String name) throws IOException;

	void startList(NbtType type, int size) throws IOException;

	void startCompound() throws IOException;

	void endTag() throws IOException;

	void ofNull() throws IOException;

	void ofByte(byte value) throws IOException;

	void ofShort(short value) throws IOException;

	void ofInt(int value) throws IOException;

	void ofLong(long value) throws IOException;

	void ofFloat(float value) throws IOException;

	void ofDouble(double value) throws IOException;

	void ofString(String value) throws IOException;

	void ofByteArray(byte[] value) throws IOException;

	void ofIntArray(int[] value) throws IOException;

	void ofLongArray(long[] value) throws IOException;

	/**
	 * Intrudes a value directly, bypassing the tree
	 */
	default void push(Nbt<?> value) throws IOException {
		switch (value.getType()) {
			case Null -> ofNull();
			case Byte -> ofByte(value.asByte());
			case Short -> ofShort(value.asShort());
			case Int -> ofInt(value.asInt());
			case Long -> ofLong(value.asLong());
			case Float -> ofFloat(value.asFloat());
			case Double -> ofDouble(value.asDouble());
			case ByteArray -> ofByteArray(value.asBytesRaw());
			case String -> ofString(value.asString());
			case List -> push((NbtList<?>) value);
			case Compound -> push((NbtCompound) value);
			case IntArray -> ofIntArray(value.asIntsRaw());
			case LongArray -> ofLongArray(value.asLongsRaw());
			default -> throw new AssertionError("unknown: " + value);
		}
	}

	default void push(NbtList<? extends Nbt<?>> list) throws IOException {
		startList(list.getComponentType(), list.size());
		switch (list.getComponentType()) {
			case Null -> {
			}
			case Byte -> {
				for (final var value : ((NbtByteList) list).toRawArray()) {
					ofByte(value);
				}
			}
			case Short -> {
				for (final var value : ((NbtShortList) list).toRawArray()) {
					ofShort(value);
				}
			}
			case Int -> {
				for (final var value : ((NbtIntList) list).toRawArray()) {
					ofInt(value);
				}
			}
			case Long -> {
				for (final var value : ((NbtLongList) list).toRawArray()) {
					ofLong(value);
				}
			}
			case Float -> {
				for (final var value : ((NbtFloatList) list).toRawArray()) {
					ofFloat(value);
				}
			}
			case Double -> {
				for (final var value : ((NbtDoubleList) list).toRawArray()) {
					ofDouble(value);
				}
			}
			default -> {
				for (final var value : (NbtGenericList<?>) list) {
					push(value);
				}
			}
		}
		endTag();
	}

	default void push(NbtCompound compound) throws IOException {
		startCompound();
		for (final var entry : compound.entries()) {
			field(entry.getKey());
			push(entry.getValue());
		}
		ofNull();
	}
}
