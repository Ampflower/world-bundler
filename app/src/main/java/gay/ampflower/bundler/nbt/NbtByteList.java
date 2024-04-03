package gay.ampflower.bundler.nbt;

import gay.ampflower.bundler.utils.transform.ByteTransformingIterator;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteCollection;
import it.unimi.dsi.fastutil.bytes.ByteIterator;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class NbtByteList extends NbtList<NbtByte> implements ByteCollection, NbtArrayList<NbtByte, Byte, byte[]> {
	private final ByteArrayList backing;

	public NbtByteList() {
		this.backing = new ByteArrayList();
	}

	public NbtByteList(int initial) {
		this.backing = new ByteArrayList(initial);
	}

	public NbtByteList(byte[] bytes) {
		this.backing = new ByteArrayList(bytes);
	}

	@Override
	public boolean remove(final Nbt<?> value) {
		return this.backing.rem(value.asByte());
	}

	@Override
	public NbtByte get(final int key) {
		return new NbtByte(backing.getByte(key));
	}

	@Override
	public byte getByte(final int key) {
		return backing.getByte(key);
	}

	@Override
	public boolean add(final NbtByte value) {
		return backing.add(value.asByte());
	}

	@Override
	public boolean add(final Byte value) {
		return backing.add(value);
	}

	@Override
	public boolean add(final boolean value) {
		return add(value ? Nbt.TRUE : Nbt.FALSE);
	}

	@Override
	public boolean add(final byte value) {
		return backing.add(value);
	}

	public boolean addAll(ByteIterator itr) {
		boolean added = false;
		while (itr.hasNext()) {
			added |= this.add(itr.nextByte());
		}
		return added;
	}

	@Override
	public NbtType getComponentType() {
		return NbtType.Byte;
	}

	@Override
	public int size() {
		return backing.size();
	}

	@Override
	public boolean isEmpty() {
		return backing.isEmpty();
	}

	@Override
	public String toString() {
		return "NbtByteList" + this.backing;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof NbtByteList other)) {
			return false;
		}
		return backing.equals(other.backing);
	}

	@Override
	public ByteIterator iterator() {
		return this.backing.iterator();
	}

	@Override
	public Iterator<NbtByte> nbtIterator() {
		return new ByteTransformingIterator<>(iterator(), NbtByte::new);
	}

	@Override
	public byte[] toRawArray() {
		return this.backing.toByteArray();
	}

	@Override
	public Object[] toArray() {
		return this.backing.toArray();
	}

	@Override
	public <T> T[] toArray(final T[] a) {
		return this.backing.toArray(a);
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		return this.backing.containsAll(c);
	}

	@Override
	public boolean addAll(final Collection<? extends Byte> c) {
		return this.backing.addAll(c);
	}

	@Override
	public boolean removeAll(final Collection<?> c) {
		return this.backing.removeAll(c);
	}

	@Override
	public boolean retainAll(final Collection<?> c) {
		return this.backing.retainAll(c);
	}

	@Override
	public void clear() {
		this.backing.clear();
	}

	@Override
	public boolean contains(final byte key) {
		return this.backing.contains(key);
	}

	@Override
	public boolean rem(final byte key) {
		return this.backing.rem(key);
	}

	@Override
	public byte[] toByteArray() {
		return this.backing.toByteArray();
	}

	@Override
	public byte[] toArray(final byte[] a) {
		return this.backing.toArray(a);
	}

	@Override
	public boolean addAll(final ByteCollection c) {
		return this.backing.addAll(c);
	}

	@Override
	public boolean containsAll(final ByteCollection c) {
		return this.backing.containsAll(c);
	}

	@Override
	public boolean removeAll(final ByteCollection c) {
		return this.backing.removeAll(c);
	}

	@Override
	public boolean retainAll(final ByteCollection c) {
		return this.backing.retainAll(c);
	}
}
