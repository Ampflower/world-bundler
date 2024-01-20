package gay.ampflower.bundler.nbt.io;

import gay.ampflower.bundler.nbt.Nbt;
import gay.ampflower.bundler.nbt.NbtType;

/**
 * @author Ampflower
 * @since ${version}
 **/
public interface SaxNbtParser {
	void field(String name);

	void startList(NbtType type, int size);

	void startCompound();

	void endTag();

	void ofNull();

	void ofByte(byte value);

	void ofShort(short value);

	void ofInt(int value);

	void ofLong(long value);

	void ofFloat(float value);

	void ofDouble(double value);

	void ofString(String value);

	void ofByteArray(byte[] value);

	void ofIntArray(int[] value);

	void ofLongArray(long[] value);

	/**
	 * Intrudes a value directly, bypassing the tree
	 */
	void push(Nbt<?> value);
}
