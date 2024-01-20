package gay.ampflower.bundler.nbt.io;

import gay.ampflower.bundler.nbt.NbtType;
import gay.ampflower.bundler.utils.ArrayUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

import static gay.ampflower.bundler.utils.ArrayUtils.*;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class NbtReader implements AutoCloseable {
	private final InputStream stream;
	private final byte[] buf = new byte[8192];
	private int ia, ib;

	private static final VarHandle
		SHORT_HANDLE = ArrayUtils.SHORTS_BIG_ENDIAN,
		INT_HANDLE = ArrayUtils.INTS_BIG_ENDIAN,
		LONG_HANDLE = ArrayUtils.LONGS_BIG_ENDIAN,
		FLOAT_HANDLE = ArrayUtils.FLOATS_BIG_ENDIAN,
		DOUBLE_HANDLE = ArrayUtils.DOUBLES_BIG_ENDIAN;

	public NbtReader(InputStream stream) {
		this.stream = stream;
	}

	private void available(int available) throws IOException {
		if (ib - ia < available) {
			alignAndRead();
		}
		if (ib == 0) throw new IOException("Not enough available");
	}

	private void alignAndRead() throws IOException {
		final int len = ib - ia;
		System.arraycopy(buf, ia, buf, 0, len);
		ib = len + stream.readNBytes(buf, len, buf.length - len);
		ia = 0;
	}

	public NbtType readType() throws IOException {
		return NbtType.byId(readByte());
	}

	public byte readByte() throws IOException {
		available(BYTE_STRIDE);
		return buf[ia++];
	}

	public short readShort() throws IOException {
		available(SHORT_STRIDE);
		final short ret = (short) SHORT_HANDLE.get(buf, ia);
		ia += SHORT_STRIDE;
		return ret;
	}

	public int readInt() throws IOException {
		available(INT_STRIDE);
		final int ret = (int) INT_HANDLE.get(buf, ia);
		ia += INT_STRIDE;
		return ret;
	}

	public long readLong() throws IOException {
		available(LONG_STRIDE);
		final long ret = (long) LONG_HANDLE.get(buf, ia);
		ia += LONG_STRIDE;
		return ret;
	}

	public float readFloat() throws IOException {
		available(FLOAT_STRIDE);
		final float ret = (float) FLOAT_HANDLE.get(buf, ia);
		ia += FLOAT_STRIDE;
		return ret;

	}

	public double readDouble() throws IOException {
		available(DOUBLE_STRIDE);
		final double ret = (double) DOUBLE_HANDLE.get(buf, ia);
		ia += DOUBLE_STRIDE;
		return ret;
	}

	private byte[] readBytes(int size) throws IOException {
		final byte[] bytes = new byte[size];
		final int bufLen = Math.min(ib - ia, size);
		System.arraycopy(buf, ia, bytes, 0, bufLen);
		final int read = stream.readNBytes(bytes, bufLen, size - bufLen);
		if (read + bufLen != size) {
			throw new IOException("Read mismatch: read " + (read + bufLen) + ", need " + size + " (buf: " + bufLen + ", str: " + read + ")");
		}
		ia += bufLen;
		return bytes;
	}

	public byte[] readByteArray() throws IOException {
		return readBytes(readInt());
	}

	public String readString() throws IOException {
		final byte[] bytes = readBytes(readShort() & 0xFFFF);
		return new String(bytes, 0, bytes.length, StandardCharsets.UTF_8);
	}

	public short[] readShortArray() throws IOException {
		final int size = readInt();
		final short[] shorts = new short[size];
		int read = 0, ic;
		while (read < size) {
			available((size - read) * SHORT_STRIDE);
			ic = Math.min((ib - ia) / SHORT_STRIDE, size);
			ArrayUtils.copyBigEndianShorts(buf, ia, shorts, 0, ic);
			read += ic;
			ia += ic * SHORT_STRIDE;
			assert ia <= ib : "Overread";
		}

		return shorts;
	}

	public int[] readIntArray() throws IOException {
		final int size = readInt();
		final int[] ints = new int[size];
		int read = 0, ic;
		while (read < size) {
			available((size - read) * INT_STRIDE);
			ic = Math.min((ib - ia) / INT_STRIDE, size);
			ArrayUtils.copyBigEndianInts(buf, ia, ints, 0, ic);
			read += ic;
			ia += ic * INT_STRIDE;
			assert ia <= ib : "Overread";
		}

		return ints;
	}

	public long[] readLongArray() throws IOException {
		final int size = readInt();
		final long[] longs = new long[size];
		int read = 0, ic;
		while (read < size) {
			available((size - read) * LONG_STRIDE);
			ic = Math.min((ib - ia) / LONG_STRIDE, size);
			ArrayUtils.copyBigEndianLongs(buf, ia, longs, 0, ic);
			read += ic;
			ia += ic * LONG_STRIDE;
			assert ia <= ib : "Overread";
		}

		return longs;
	}

	@Override
	public void close() throws IOException {
		stream.close();
	}
}
