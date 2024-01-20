package gay.ampflower.bundler.nbt;

import java.util.function.IntFunction;

/**
 * @author Ampflower
 * @since ${version}
 **/
public enum NbtType {
	Null(0, Void[]::new),
	Byte(1, byte[]::new),
	Short(2, short[]::new),
	Int(3, int[]::new),
	Long(4, long[]::new),
	Float(5, float[]::new),
	Double(6, double[]::new),
	ByteArray(7, NbtByteArray[]::new),
	String(8, String[]::new),
	List(9, NbtList[]::new),
	Compound(10, NbtCompound[]::new),
	IntArray(11, NbtIntArray[]::new),
	LongArray(12, NbtLongArray[]::new),
	;

	private static final NbtType[] types = values();
	public final int type;
	public final IntFunction<Object> genArray;

	NbtType(int type, IntFunction<Object> genArray) {
		this.type = type;
		this.genArray = genArray;
	}

	public static NbtType byId(int id) {
		return types[id];
	}
}
