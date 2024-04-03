package gay.ampflower.bundler.nbt;

import gay.ampflower.bundler.utils.transform.FloatTransformingIterator;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatCollection;
import it.unimi.dsi.fastutil.floats.FloatIterator;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class NbtFloatList extends NbtList<NbtFloat> implements FloatCollection, NbtArrayList<NbtFloat, Float, float[]> {
	private final FloatArrayList backing;

	public NbtFloatList() {
		this.backing = new FloatArrayList();
	}

	public NbtFloatList(int initial) {
		this.backing = new FloatArrayList(initial);
	}

	public NbtFloatList(float[] floats) {
		this.backing = new FloatArrayList(floats);
	}

	@Override
	public boolean remove(final Nbt<?> value) {
		return this.backing.rem(value.asFloat());
	}

	@Override
	public NbtFloat get(final int key) {
		return new NbtFloat(backing.getFloat(key));
	}

	@Override
	public float getFloat(final int key) {
		return backing.getFloat(key);
	}

	@Override
	public boolean add(final NbtFloat value) {
		return backing.add(value.asFloat());
	}

	@Override
	public boolean add(final Float value) {
		return backing.add(value);
	}

	@Override
	public boolean add(final float value) {
		return backing.add(value);
	}

	public boolean addAll(FloatIterator itr) {
		boolean added = false;
		while (itr.hasNext()) {
			added |= this.add(itr.nextFloat());
		}
		return added;
	}

	@Override
	public NbtType getComponentType() {
		return NbtType.Float;
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
		return "NbtFloatList" + this.backing;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof NbtFloatList other)) {
			return false;
		}
		return backing.equals(other.backing);
	}

	@Override
	public FloatIterator iterator() {
		return this.backing.iterator();
	}

	@Override
	public Iterator<NbtFloat> nbtIterator() {
		return new FloatTransformingIterator<>(iterator(), NbtFloat::new);
	}

	@Override
	public float[] toRawArray() {
		return this.backing.toFloatArray();
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
	public boolean addAll(final Collection<? extends Float> c) {
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
	public boolean contains(final float key) {
		return this.backing.contains(key);
	}

	@Override
	public boolean rem(final float key) {
		return this.backing.rem(key);
	}

	@Override
	public float[] toFloatArray() {
		return this.backing.toFloatArray();
	}

	@Override
	public float[] toArray(final float[] a) {
		return this.backing.toArray(a);
	}

	@Override
	public boolean addAll(final FloatCollection c) {
		return this.backing.addAll(c);
	}

	@Override
	public boolean containsAll(final FloatCollection c) {
		return this.backing.containsAll(c);
	}

	@Override
	public boolean removeAll(final FloatCollection c) {
		return this.backing.removeAll(c);
	}

	@Override
	public boolean retainAll(final FloatCollection c) {
		return this.backing.retainAll(c);
	}
}
