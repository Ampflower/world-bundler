package gay.ampflower.bundler.nbt;

import gay.ampflower.bundler.utils.transform.DoubleTransformingIterator;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleCollection;
import it.unimi.dsi.fastutil.doubles.DoubleIterator;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class NbtDoubleList extends NbtList<NbtDouble> implements DoubleCollection, NbtArrayList<NbtDouble, Double, double[]> {
	private final DoubleArrayList backing;

	public NbtDoubleList() {
		this.backing = new DoubleArrayList();
	}

	public NbtDoubleList(int initial) {
		this.backing = new DoubleArrayList(initial);
	}

	public NbtDoubleList(double[] doubles) {
		this.backing = new DoubleArrayList(doubles);
	}

	@Override
	public boolean remove(final Nbt<?> value) {
		return this.backing.rem(value.asDouble());
	}

	@Override
	public NbtDouble get(final int key) {
		return new NbtDouble(backing.getDouble(key));
	}

	@Override
	public double getDouble(final int key) {
		return backing.getDouble(key);
	}

	@Override
	public boolean add(final NbtDouble value) {
		return backing.add(value.asDouble());
	}

	@Override
	public boolean add(final Double value) {
		return backing.add(value);
	}

	@Override
	public boolean add(final double value) {
		return backing.add(value);
	}

	public boolean addAll(DoubleIterator itr) {
		boolean added = false;
		while (itr.hasNext()) {
			added |= this.add(itr.nextDouble());
		}
		return added;
	}

	@Override
	public NbtType getComponentType() {
		return NbtType.Double;
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
		return "NbtDoubleList" + this.backing;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof NbtDoubleList other)) {
			return false;
		}
		return backing.equals(other.backing);
	}

	@Override
	public DoubleIterator iterator() {
		return this.backing.iterator();
	}

	@Override
	public Iterator<NbtDouble> nbtIterator() {
		return new DoubleTransformingIterator<>(iterator(), NbtDouble::new);
	}

	@Override
	public double[] toRawArray() {
		return this.backing.toDoubleArray();
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
	public boolean addAll(final Collection<? extends Double> c) {
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
	public boolean contains(final double key) {
		return this.backing.contains(key);
	}

	@Override
	public boolean rem(final double key) {
		return this.backing.rem(key);
	}

	@Override
	public double[] toDoubleArray() {
		return this.backing.toDoubleArray();
	}

	@Override
	public double[] toArray(final double[] a) {
		return this.backing.toArray(a);
	}

	@Override
	public boolean addAll(final DoubleCollection c) {
		return this.backing.addAll(c);
	}

	@Override
	public boolean containsAll(final DoubleCollection c) {
		return this.backing.containsAll(c);
	}

	@Override
	public boolean removeAll(final DoubleCollection c) {
		return this.backing.removeAll(c);
	}

	@Override
	public boolean retainAll(final DoubleCollection c) {
		return this.backing.retainAll(c);
	}
}
