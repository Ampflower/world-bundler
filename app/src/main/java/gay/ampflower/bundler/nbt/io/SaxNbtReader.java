package gay.ampflower.bundler.nbt.io;

import gay.ampflower.bundler.nbt.*;
import gay.ampflower.bundler.utils.ArrayUtils;

import java.io.IOException;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class SaxNbtReader {

	public static void parse(SaxNbtParser parser, NbtReader reader) throws IOException {
		final var type = reader.readType();
		parser.field(reader.readString());
		jmp(parser, reader, type);
	}

	private static void jmp(SaxNbtParser parser, NbtReader reader, NbtType type) throws IOException {
		switch (type) {
			case Null -> parser.ofNull();
			case Byte -> parser.ofByte(reader.readByte());
			case Short -> parser.ofShort(reader.readShort());
			case Int -> parser.ofInt(reader.readInt());
			case Long -> parser.ofLong(reader.readLong());
			case Float -> parser.ofFloat(reader.readFloat());
			case Double -> parser.ofDouble(reader.readDouble());
			case ByteArray -> parser.ofByteArray(reader.readByteArray());
			case String -> parser.ofString(reader.readString());
			case List -> readList(parser, reader, reader.readType());
			case Compound -> {
				parser.startCompound();
				readCompound(parser, reader);
			}
			case IntArray -> parser.ofIntArray(reader.readIntArray());
			case LongArray -> parser.ofLongArray(reader.readLongArray());
		}
	}

	private static void readList(SaxNbtParser parser, NbtReader reader, NbtType expected) throws IOException {
		switch (expected) {
			case Null -> {
				final int count = reader.readInt();
				if (count != 0) {
					throw new AssertionError("Non-zero nulls: " + count);
				}
				parser.push(NbtList.empty());
			}
			case Byte -> parser.push(new NbtByteList(reader.readByteArray()));
			case Short -> parser.push(new NbtShortList(reader.readShortArray()));
			case Int -> parser.push(new NbtIntList(reader.readIntArray()));
			case Long -> parser.push(new NbtLongList(reader.readLongArray()));
			case Float -> {
				final var ints = reader.readIntArray();
				final var floats = ArrayUtils.intsAsFloats(ints);
				parser.push(new NbtFloatList(floats));
			}
			case Double -> {
				final var longs = reader.readLongArray();
				final var doubles = ArrayUtils.longsAsDoubles(longs);
				parser.push(new NbtDoubleList(doubles));
			}
			default -> {
				final int count = reader.readInt();
				parser.startList(expected, count);
				switch (expected) {
					case ByteArray -> {
						for (int i = 0; i < count; i++) {
							parser.ofByteArray(reader.readByteArray());
						}
					}
					case String -> {
						for (int i = 0; i < count; i++) {
							parser.ofString(reader.readString());
						}
					}
					case List -> {
						for (int i = 0; i < count; i++) {
							readList(parser, reader, reader.readType());
						}
					}
					case Compound -> {
						for (int i = 0; i < count; i++) {
							parser.startCompound();
							readCompound(parser, reader);
						}
					}
					case IntArray -> {
						for (int i = 0; i < count; i++) {
							parser.ofIntArray(reader.readIntArray());
						}
					}
					case LongArray -> {
						for (int i = 0; i < count; i++) {
							parser.ofLongArray(reader.readLongArray());
						}
					}
				}
				parser.endTag();
			}
		}
	}

	private static void readCompound(SaxNbtParser parser, NbtReader reader) throws IOException {
		while (true) {
			final var type = reader.readType();
			if (type == NbtType.Null) {
				parser.ofNull();
				return;
			}
			parser.field(reader.readString());
			jmp(parser, reader, type);
		}
	}
}
