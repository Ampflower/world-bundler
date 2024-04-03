package gay.ampflower.bundler.nbt;

import gay.ampflower.bundler.utils.transform.LongTransformingIterator;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class NbtLongList extends NbtList<NbtLong> implements LongCollection, NbtArrayList<NbtLong, Long, long[]> {
	private final LongArrayList backing;

	public NbtLongList() {
		this.backing = new LongArrayList();
	}

	public NbtLongList(int initial) {
		this.backing = new LongArrayList(initial);
	}

	public NbtLongList(long[] longs) {
		this.backing = new LongArrayList(longs);
	}

	@Override
	public boolean remove(final Nbt<?> value) {
		return this.backing.rem(value.asLong());
	}

	@Override
	public NbtLong get(final int key) {
		return new NbtLong(backing.getLong(key));
	}

	@Override
	public long getLong(final int key) {
		return backing.getLong(key);
	}

	@Override
	public boolean add(final NbtLong value) {
		return backing.add(value.asLong());
	}

	@Override
	public boolean add(final Long value) {
		return backing.add(value);
	}

	@Override
	public boolean add(final long value) {
		return backing.add(value);
	}

	public boolean addAll(LongIterator itr) {
		boolean added = false;
		while (itr.hasNext()) {
			added |= this.add(itr.nextLong());
		}
		return added;
	}

	@Override
	public NbtType getComponentType() {
		return NbtType.Long;
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
		return "NbtLongList" + this.backing;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof NbtLongList other)) {
			return false;
		}
		return backing.equals(other.backing);
	}

	@Override
	public LongIterator iterator() {
		return this.backing.iterator();
	}

	@Override
	public Iterator<NbtLong> nbtIterator() {
		return new LongTransformingIterator<>(iterator(), NbtLong::new);
	}

	@Override
	public long[] toRawArray() {
		return this.backing.toLongArray();
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
	public boolean addAll(final Collection<? extends Long> c) {
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
	public boolean contains(final long key) {
		return this.backing.contains(key);
	}

	@Override
	public boolean rem(final long key) {
		return this.backing.rem(key);
	}

	@Override
	public long[] toLongArray() {
		return this.backing.toLongArray();
	}

	@Override
	public long[] toArray(final long[] a) {
		return this.backing.toArray(a);
	}

	@Override
	public boolean addAll(final LongCollection c) {
		return this.backing.addAll(c);
	}

	@Override
	public boolean containsAll(final LongCollection c) {
		return this.backing.containsAll(c);
	}

	@Override
	public boolean removeAll(final LongCollection c) {
		return this.backing.removeAll(c);
	}

	@Override
	public boolean retainAll(final LongCollection c) {
		return this.backing.retainAll(c);
	}
}
