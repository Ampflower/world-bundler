package gay.ampflower.bundler.nbt.io;

import gay.ampflower.bundler.nbt.*;
import gay.ampflower.bundler.utils.ArrayUtils;
import gay.ampflower.bundler.utils.io.DataUtils;
import gay.ampflower.bundler.utils.io.IoUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.invoke.VarHandle;
import java.util.BitSet;

import static gay.ampflower.bundler.utils.ArrayUtils.*;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class NbtWriter implements AutoCloseable, SaxNbtParser {
	private final byte[] buf = new byte[8192];
	private int index;
	private int bitIndex;
	private String field;
	private final BitSet bitLists = new BitSet();
	private final OutputStream stream;

	private static final VarHandle
		SHORT_HANDLE = ArrayUtils.SHORTS_BIG_ENDIAN,
		INT_HANDLE = ArrayUtils.INTS_BIG_ENDIAN,
		LONG_HANDLE = ArrayUtils.LONGS_BIG_ENDIAN,
		FLOAT_HANDLE = ArrayUtils.FLOATS_BIG_ENDIAN,
		DOUBLE_HANDLE = ArrayUtils.DOUBLES_BIG_ENDIAN;

	private static final int
		bitIndexMask = 0x7FFFFFFF,
		bitIndexList = 0x80000000;

	public NbtWriter(OutputStream stream) {
		this.stream = stream;
	}

	private void available(int available) throws IOException {
		if (buf.length - index < available) {
			flush();
		}
	}

	private void flush() throws IOException {
		stream.write(buf, 0, index);
		index = 0;
	}

	private void writeType(NbtType type) {
		buf[index++] = type.type;
	}

	@Override
	public void close() throws IOException {
		try (stream) {
			flush();
		}
	}

	@Override
	public void field(final String name) {
		this.field = name;
	}

	private void writeField(NbtType type) throws IOException {
		if (this.bitIndex < 0) return;
		byte[] bytes = DataUtils.writeString(this.field);
		available(bytes.length + SHORT_STRIDE + BYTE_STRIDE + type.stride);
		writeType(type);
		writeString(bytes);
	}

	@Override
	public void startList(final NbtType type, final int size) throws IOException {
		writeField(NbtType.List);
		final int newBitIndex = (bitIndex & bitIndexMask) + 1;
		this.bitIndex = newBitIndex | bitIndexList;
		this.bitLists.set(newBitIndex, true);

		writeType(type);
		INT_HANDLE.set(buf, index, size);
		index += INT_STRIDE;
	}

	@Override
	public void startCompound() throws IOException {
		writeField(NbtType.Compound);
		final int newBitIndex = (bitIndex & bitIndexMask) + 1;
		this.bitIndex = newBitIndex;
		this.bitLists.set(newBitIndex, false);
	}

	private void pushStack(boolean list) {
		final int newBitIndex = (bitIndex & bitIndexMask) + 1;
		final int isList = list ? bitIndexList : 0;
		this.bitIndex = newBitIndex | isList;
		this.bitLists.set(newBitIndex, list);
	}

	@Override
	public void endTag() {
		final int newBitIndex = (bitIndex & bitIndexMask) - 1;
		final int listBit = bitLists.get(newBitIndex) ? bitIndexList : 0;
		this.bitIndex = newBitIndex | listBit;
	}

	@Override
	public void ofNull() throws IOException {
		available(BYTE_STRIDE);
		buf[index++] = 0;

		if (this.bitIndex >= 0) {
			endTag();
		}
	}

	@Override
	public void ofByte(final byte value) throws IOException {
		writeField(NbtType.Byte);
		this.buf[index++] = value;
	}

	@Override
	public void ofShort(final short value) throws IOException {
		writeField(NbtType.Short);
		SHORT_HANDLE.set(buf, index, value);
		index += SHORT_STRIDE;
	}

	@Override
	public void ofInt(final int value) throws IOException {
		writeField(NbtType.Int);
		INT_HANDLE.set(buf, index, value);
		index += INT_STRIDE;
	}

	@Override
	public void ofLong(final long value) throws IOException {
		writeField(NbtType.Long);
		LONG_HANDLE.set(buf, index, value);
		index += LONG_STRIDE;
	}

	@Override
	public void ofFloat(final float value) throws IOException {
		writeField(NbtType.Float);
		FLOAT_HANDLE.set(buf, index, value);
		index += FLOAT_STRIDE;
	}

	@Override
	public void ofDouble(final double value) throws IOException {
		writeField(NbtType.Double);
		DOUBLE_HANDLE.set(buf, index, value);
		index += DOUBLE_STRIDE;
	}

	@Override
	public void ofString(final String value) throws IOException {
		writeField(NbtType.String);
		byte[] bytes = DataUtils.writeString(value);
		available(SHORT_STRIDE + bytes.length);
		writeString(bytes);
	}

	private void writeString(final byte[] value) throws IOException {
		/*
		SHORT_HANDLE.set(buf, index, (short)value.length);
		index += SHORT_STRIDE;
		*/
		flush();
		stream.write(value);
	}

	@Override
	public void ofByteArray(final byte[] value) throws IOException {
		writeField(NbtType.ByteArray);
		writeBytes(value);
	}

	private void writeBytes(final byte[] value) throws IOException {
		available(value.length * BYTE_STRIDE + INT_STRIDE);
		INT_HANDLE.set(buf, index, value.length);
		index += INT_STRIDE;
		flush();
		stream.write(value);
	}

	private void writeShorts(final short[] value) throws IOException {
		available(value.length * SHORT_STRIDE + INT_STRIDE);
		INT_HANDLE.set(buf, index, value.length);
		index += INT_STRIDE;
		flush();
		IoUtils.writeShortsBigEndian(stream, buf, value);
	}

	@Override
	public void ofIntArray(final int[] value) throws IOException {
		writeField(NbtType.IntArray);
		writeInts(value);
	}

	private void writeInts(final int[] value) throws IOException {
		available(value.length * INT_STRIDE + INT_STRIDE);
		INT_HANDLE.set(buf, index, value.length);
		index += INT_STRIDE;
		flush();
		IoUtils.writeIntsBigEndian(stream, buf, value);
	}

	@Override
	public void ofLongArray(final long[] value) throws IOException {
		writeField(NbtType.LongArray);
		writeLongs(value);
	}

	private void writeLongs(final long[] value) throws IOException {
		available(value.length * LONG_STRIDE + INT_STRIDE);
		INT_HANDLE.set(buf, index, value.length);
		index += INT_STRIDE;
		flush();
		IoUtils.writeLongsBigEndian(stream, buf, value);
	}

	private void writeFloats(final float[] value) throws IOException {
		available(value.length * FLOAT_STRIDE + INT_STRIDE);
		INT_HANDLE.set(buf, index, value.length);
		index += INT_STRIDE;
		flush();
		IoUtils.writeFloatsBigEndian(stream, buf, value);
	}

	private void writeDoubles(final double[] value) throws IOException {
		available(value.length * DOUBLE_STRIDE + INT_STRIDE);
		INT_HANDLE.set(buf, index, value.length);
		index += INT_STRIDE;
		flush();
		IoUtils.writeDoublesBigEndian(stream, buf, value);
	}

	@Override
	public void push(final NbtList<? extends Nbt<?>> list) throws IOException {
		writeField(NbtType.List);
		final NbtType component = list.getComponentType();
		available(NbtType.List.stride);
		writeType(component);
		switch (list.getComponentType()) {
			case Null -> {
				INT_HANDLE.set(buf, index, list.size());
				index += INT_STRIDE;
			}
			case Byte -> writeBytes(((NbtByteList) list).toRawArray());
			case Short -> writeShorts(((NbtShortList) list).toRawArray());
			case Int -> writeInts(((NbtIntList) list).toRawArray());
			case Long -> writeLongs(((NbtLongList) list).toRawArray());
			case Float -> writeFloats(((NbtFloatList) list).toRawArray());
			case Double -> writeDoubles(((NbtDoubleList) list).toRawArray());
			default -> {
				pushStack(true);
				INT_HANDLE.set(buf, index, list.size());
				index += INT_STRIDE;
				for (final var value : (NbtGenericList<?>) list) {
					push(value);
				}
				endTag();
			}
		}
	}
}
