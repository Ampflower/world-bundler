package gay.ampflower.bundler.data.common;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * @author Ampflower
 **/
public abstract class ListProxy<T> implements List<T> {
	protected final ArrayList<T> backing;

	protected ListProxy(final ArrayList<T> backing) {
		this.backing = backing;
	}

	@Override
	public final T get(final int index) {
		return this.backing.get(index);
	}

	@Override
	public final boolean add(final T value) {
		return this.backing.add(value);
	}

	@Override
	public final boolean remove(final Object value) {
		return this.backing.remove(value);
	}

	@Override
	public final boolean containsAll(final @NotNull Collection<?> values) {
		return this.backing.containsAll(values);
	}

	@Override
	public final boolean addAll(final @NotNull Collection<? extends T> values) {
		return this.backing.addAll(values);
	}

	@Override
	public final boolean addAll(final int index, @NotNull final Collection<? extends T> c) {
		return this.backing.addAll(index, c);
	}

	@Override
	public final T set(final int index, final T element) {
		return this.backing.set(index, element);
	}

	@Override
	public final void add(final int index, final T element) {
		this.backing.add(index, element);
	}

	@Override
	public final T remove(final int index) {
		return this.backing.remove(index);
	}

	@Override
	public final int indexOf(final Object o) {
		return this.backing.indexOf(o);
	}

	@Override
	public final int lastIndexOf(final Object o) {
		return this.backing.lastIndexOf(o);
	}

	@NotNull
	@Override
	public final ListIterator<T> listIterator() {
		return this.backing.listIterator();
	}

	@NotNull
	@Override
	public final ListIterator<T> listIterator(final int index) {
		return this.backing.listIterator(index);
	}

	@NotNull
	@Override
	public final List<T> subList(final int fromIndex, final int toIndex) {
		return this.backing.subList(fromIndex, toIndex);
	}

	@Override
	public final boolean removeAll(final @NotNull Collection<?> values) {
		return this.backing.removeAll(values);
	}

	@Override
	public final boolean retainAll(final @NotNull Collection<?> values) {
		return this.backing.retainAll(values);
	}

	@Override
	public final void clear() {
		this.backing.clear();
	}

	@Override
	public final int size() {
		return this.backing.size();
	}

	@Override
	public final boolean isEmpty() {
		return this.backing.isEmpty();
	}

	@Override
	public final boolean contains(final Object value) {
		return this.backing.contains(value);
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof ListProxy<?> other)) {
			return false;
		}
		return this.backing.equals(other.backing);
	}

	@Override
	@NotNull
	public final Iterator<T> iterator() {
		return this.backing.iterator();
	}

	@Override
	public final Object @NotNull [] toArray() {
		return this.backing.toArray();
	}

	@Override
	public final <T1> T1 @NotNull [] toArray(final T1 @NotNull [] array) {
		return this.backing.toArray(array);
	}
}
