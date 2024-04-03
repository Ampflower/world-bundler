package gay.ampflower.bundler.nbt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class NbtGenericList<T extends Nbt<?>> extends NbtList<T> implements Collection<T> {
	private final ArrayList<T> backing;
	private final NbtType type;

	NbtGenericList(NbtType type, int size) {
		this.backing = new ArrayList<>(size);
		this.type = type;
	}

	@Override
	public T get(final int key) {
		return this.backing.get(key);
	}

	@Override
	public boolean add(final T nbt) {
		return this.backing.add(nbt);
	}

	@Override
	public boolean remove(final Object o) {
		return this.backing.remove(o);
	}

	@Override
	public boolean containsAll(final Collection<?> c) {
		return this.backing.containsAll(c);
	}

	@Override
	public boolean addAll(final Collection<? extends T> c) {
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
	public NbtType getComponentType() {
		return type;
	}

	@Override
	public boolean remove(final Nbt<?> nbt) {
		return this.backing.remove(nbt);
	}

	@Override
	public int size() {
		return this.backing.size();
	}

	@Override
	public boolean contains(final Object o) {
		return this.backing.contains(o);
	}

	@Override
	public String toString() {
		return "NbtList<" + this.type + ">" + this.backing;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof NbtGenericList<?> other)) {
			return false;
		}
		return this.backing.equals(other.backing);
	}

	@Override
	public Iterator<T> iterator() {
		return nbtIterator();
	}

	@Override
	public Object[] toArray() {
		return this.backing.toArray();
	}

	@Override
	public <T1> T1[] toArray(final T1[] a) {
		return this.backing.toArray(a);
	}

	@Override
	public Iterator<T> nbtIterator() {
		return this.backing.iterator();
	}
}
