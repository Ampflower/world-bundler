package gay.ampflower.bundler.nbt;

import gay.ampflower.bundler.utils.transform.IntTransformingIterator;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class NbtIntList extends NbtList<NbtInt> implements IntCollection, NbtArrayList<NbtInt, Integer, int[]> {
	private final IntArrayList backing;

	public NbtIntList() {
		this.backing = new IntArrayList();
	}

	public NbtIntList(int initial) {
		this.backing = new IntArrayList(initial);
	}

	public NbtIntList(int[] ints) {
		this.backing = new IntArrayList(ints);
	}

	@Override
	public boolean remove(final Nbt<?> value) {
		return this.backing.rem(value.asInt());
	}

	@Override
	public NbtInt get(final int key) {
		return new NbtInt(backing.getInt(key));
	}

	@Override
	public int getInt(final int key) {
		return backing.getInt(key);
	}

	@Override
	public boolean add(final NbtInt value) {
		return backing.add(value.asInt());
	}

	@Override
	public boolean add(final Integer value) {
		return backing.add(value);
	}

	@Override
	public boolean add(final int value) {
		return backing.add(value);
	}

	public boolean addAll(IntIterator itr) {
		boolean added = false;
		while (itr.hasNext()) {
			added |= this.add(itr.nextInt());
		}
		return added;
	}

	@Override
	public NbtType getComponentType() {
		return NbtType.Int;
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
		return "NbtIntList" + this.backing;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof NbtIntList other)) {
			return false;
		}
		return backing.equals(other.backing);
	}

	@Override
	public IntIterator iterator() {
		return this.backing.iterator();
	}

	@Override
	public Iterator<NbtInt> nbtIterator() {
		return new IntTransformingIterator<>(iterator(), NbtInt::new);
	}

	@Override
	public int[] toRawArray() {
		return this.backing.toIntArray();
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
	public boolean addAll(final Collection<? extends Integer> c) {
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
	public boolean contains(final int key) {
		return this.backing.contains(key);
	}

	@Override
	public boolean rem(final int key) {
		return this.backing.rem(key);
	}

	@Override
	public int[] toIntArray() {
		return this.backing.toIntArray();
	}

	@Override
	public int[] toArray(final int[] a) {
		return this.backing.toArray(a);
	}

	@Override
	public boolean addAll(final IntCollection c) {
		return this.backing.addAll(c);
	}

	@Override
	public boolean containsAll(final IntCollection c) {
		return this.backing.containsAll(c);
	}

	@Override
	public boolean removeAll(final IntCollection c) {
		return this.backing.removeAll(c);
	}

	@Override
	public boolean retainAll(final IntCollection c) {
		return this.backing.retainAll(c);
	}
}
