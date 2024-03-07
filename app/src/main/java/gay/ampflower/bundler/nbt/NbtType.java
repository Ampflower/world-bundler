package gay.ampflower.bundler.nbt;

import java.util.function.IntFunction;

import static gay.ampflower.bundler.utils.ArrayUtils.*;

/**
 * @author Ampflower
 * @since ${version}
 **/
public enum NbtType {
	Null(0, 0, Void[]::new),
	Byte(1, BYTE_STRIDE, byte[]::new),
	Short(2, SHORT_STRIDE, short[]::new),
	Int(3, INT_STRIDE, int[]::new),
	Long(4, LONG_STRIDE, long[]::new),
	Float(5, FLOAT_STRIDE, float[]::new),
	Double(6, DOUBLE_STRIDE, double[]::new),
	ByteArray(7, INT_STRIDE, NbtByteArray[]::new),
	String(8, SHORT_STRIDE, String[]::new),
	List(9, BYTE_STRIDE + INT_STRIDE, NbtList[]::new),
	Compound(10, 0, NbtCompound[]::new),
	IntArray(11, INT_STRIDE, NbtIntArray[]::new),
	LongArray(12, INT_STRIDE, NbtLongArray[]::new),
	;

	private static final NbtType[] types = values();
	public final byte type;
	public final int stride;
	public final IntFunction<Object> genArray;

	NbtType(int type, int stride, IntFunction<Object> genArray) {
		this.type = (byte) type;
		this.stride = stride;
		this.genArray = genArray;
	}

	public static NbtType byId(int id) {
		return types[id];
	}
}
