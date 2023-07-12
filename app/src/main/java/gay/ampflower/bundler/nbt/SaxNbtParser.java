package gay.ampflower.bundler.nbt;

/**
 * @author Ampflower
 * @since ${version}
 **/
public interface SaxNbtParser {
    void field(String name);
    void startList(NbtType type);
    void endList();
    void startCompound();
    void endCompound();

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
}
