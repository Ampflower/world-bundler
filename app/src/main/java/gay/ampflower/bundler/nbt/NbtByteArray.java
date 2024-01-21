package gay.ampflower.bundler.nbt;

import java.util.Arrays;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class NbtByteArray implements Nbt<byte[]> {
	private static final byte[] SENTINEL = {};

	private byte[] backing;

	public NbtByteArray() {
		this.backing = SENTINEL;
	}

	public NbtByteArray(int size) {
		this.backing = new byte[size];
	}

	public NbtByteArray(byte[] bytes) {
		this.backing = bytes;
	}

	@Override
	public byte[] asBytesRaw() {
		return this.backing;
	}

	public void setBytes(byte[] bytes) {
		this.backing = bytes;
	}

	@Override
	public NbtType getType() {
		return NbtType.ByteArray;
	}

	@Override
	public StringBuilder asStringifiedNbt(final StringBuilder builder) {
		return NbtUtil.truncWith(NbtUtil.toString(builder.append("[B;"), backing), ']');
	}

	@Override
	public String toString() {
		return "NbtByteArray" + Arrays.toString(this.backing);
	}
}
