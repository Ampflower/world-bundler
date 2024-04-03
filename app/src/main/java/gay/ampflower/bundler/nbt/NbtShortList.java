package gay.ampflower.bundler.nbt;

import gay.ampflower.bundler.utils.transform.ShortTransformingIterator;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortCollection;
import it.unimi.dsi.fastutil.shorts.ShortIterator;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class NbtShortList extends NbtList<NbtShort> implements ShortCollection, NbtArrayList<NbtShort, Short, short[]> {
	private final ShortArrayList backing;

	public NbtShortList() {
		this.backing = new ShortArrayList();
	}

	public NbtShortList(int initial) {
		this.backing = new ShortArrayList(initial);
	}

	public NbtShortList(short[] shorts) {
		this.backing = new ShortArrayList(shorts);
	}

	@Override
	public boolean remove(final Nbt<?> value) {
		return this.backing.rem(value.asShort());
	}

	@Override
	public NbtShort get(final int key) {
		return new NbtShort(backing.getShort(key));
	}

	@Override
	public short getShort(final int key) {
		return backing.getShort(key);
	}

	@Override
	public boolean add(final NbtShort value) {
		return backing.add(value.asShort());
	}

	@Override
	public boolean add(final Short value) {
		return backing.add(value);
	}

	@Override
	public boolean add(final short value) {
		return backing.add(value);
	}

	public boolean addAll(ShortIterator itr) {
		boolean added = false;
		while (itr.hasNext()) {
			added |= this.add(itr.nextShort());
		}
		return added;
	}

	@Override
	public NbtType getComponentType() {
		return NbtType.Short;
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
		return "NbtShortList" + this.backing;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof NbtShortList other)) {
			return false;
		}
		return backing.equals(other.backing);
	}

	@Override
	public ShortIterator iterator() {
		return this.backing.iterator();
	}

	@Override
	public Iterator<NbtShort> nbtIterator() {
		return new ShortTransformingIterator<>(iterator(), NbtShort::new);
	}

	@Override
	public short[] toRawArray() {
		return this.backing.toShortArray();
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
	public boolean addAll(final Collection<? extends Short> c) {
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
	public boolean contains(final short key) {
		return this.backing.contains(key);
	}

	@Override
	public boolean rem(final short key) {
		return this.backing.rem(key);
	}

	@Override
	public short[] toShortArray() {
		return this.backing.toShortArray();
	}

	@Override
	public short[] toArray(final short[] a) {
		return this.backing.toArray(a);
	}

	@Override
	public boolean addAll(final ShortCollection c) {
		return this.backing.addAll(c);
	}

	@Override
	public boolean containsAll(final ShortCollection c) {
		return this.backing.containsAll(c);
	}

	@Override
	public boolean removeAll(final ShortCollection c) {
		return this.backing.removeAll(c);
	}

	@Override
	public boolean retainAll(final ShortCollection c) {
		return this.backing.retainAll(c);
	}
}
