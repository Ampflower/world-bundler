package gay.ampflower.bundler.nbt.io;

import gay.ampflower.bundler.nbt.Nbt;
import gay.ampflower.bundler.nbt.NbtType;

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
	void push(Nbt<?> value) throws IOException;
}
