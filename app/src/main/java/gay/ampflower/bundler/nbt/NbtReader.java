package gay.ampflower.bundler.nbt;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class NbtReader implements AutoCloseable {
	static final VarHandle SHORTS = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.BIG_ENDIAN);
	static final VarHandle CHARS = MethodHandles.byteArrayViewVarHandle(char[].class, ByteOrder.BIG_ENDIAN);
	static final VarHandle INTS = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);
	static final VarHandle LONGS = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);
	static final VarHandle FLOATS = MethodHandles.byteArrayViewVarHandle(float[].class, ByteOrder.BIG_ENDIAN);
	static final VarHandle DOUBLES = MethodHandles.byteArrayViewVarHandle(double[].class, ByteOrder.BIG_ENDIAN);
/*
	public String readKey() {

	}

	public void readNull() {

	}

	public byte readByte() {

	}

	public short readShort() {

	}

	public char readChar() {

	}

	public int readInt() {
		;
	}

	public long readLong() {
		;
	}

	public float readFloat() {

	}

	public double readDouble() {

	}

	public byte[] readByteArray() {

	}

	public String readString() {

	}

	public List<?> readList() {

	}

	public Map<String, ?> readCompound() {

	}

	public int[] readIntArray() {

	}

	public long[] readLongArray() {

	}*/

	@Override
	public void close() throws Exception {

	}
}
