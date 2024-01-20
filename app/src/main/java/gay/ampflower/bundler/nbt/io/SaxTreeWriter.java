package gay.ampflower.bundler.nbt.io;

import gay.ampflower.bundler.nbt.*;
import gay.ampflower.bundler.utils.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class SaxTreeWriter implements SaxNbtParser {
	private static final Logger logger = LogUtils.logger();

	private final Deque<Nbt<?>> elements = new ArrayDeque<>();
	private String field, rootName;
	private Nbt<?> current, root;

	public Nbt<?> getRoot() {
		return root;
	}

	public String getRootName() {
		return rootName;
	}

	@Override
	public void field(final String name) {
		this.field = name;
	}

	@Override
	public void startList(final NbtType type, final int size) {
		pushLast(new NbtList<>(size, type));
	}

	@Override
	public void startCompound() {
		pushLast(new NbtCompound());
	}

	@Override
	public void endTag() {
		logger.trace("Called with {} elements @ {}", elements.size(), current, new Throwable());
		this.current = elements.poll();
	}

	@Override
	public void ofNull() {
		if (this.current.getType() == NbtType.Compound) {
			endTag();
			return;
		}
		push(NbtNull.Null);
	}

	private void pushLast(Nbt<?> tag) {
		final var last = this.current;
		if (last != null) {
			elements.push(last);
			last.push(field, tag);
		} else {
			root = tag;
			rootName = field;
		}
		this.current = tag;
	}

	@Override
	public void push(Nbt<?> value) {
		if (current == null) {
			if (!(value instanceof NbtCompound)) {
				throw new AssertionError("got " + value + " at root");
			}
			rootName = field;
			root = current = value;
			return;
		}
		this.current.push(this.field, value);
	}

	@Override
	public void ofByte(final byte value) {
		push(new NbtByte(value));
	}

	@Override
	public void ofShort(final short value) {
		push(new NbtShort(value));
	}

	@Override
	public void ofInt(final int value) {
		push(new NbtInt(value));
	}

	@Override
	public void ofLong(final long value) {
		push(new NbtLong(value));
	}

	@Override
	public void ofFloat(final float value) {
		push(new NbtFloat(value));
	}

	@Override
	public void ofDouble(final double value) {
		push(new NbtDouble(value));
	}

	@Override
	public void ofString(final String value) {
		push(new NbtString(value));
	}

	@Override
	public void ofByteArray(final byte[] value) {
		push(new NbtByteArray(value));
	}

	@Override
	public void ofIntArray(final int[] value) {
		push(new NbtIntArray(value));
	}

	@Override
	public void ofLongArray(final long[] value) {
		push(new NbtLongArray(value));
	}
}
