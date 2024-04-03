package gay.ampflower.bundler.nbt;

import java.util.Iterator;
import java.util.List;

/**
 * @author Ampflower
 * @since ${version}
 **/
sealed interface NbtArrayList<T extends Nbt<V>, V, R> extends Nbt<List<T>> permits NbtByteList, NbtShortList, NbtIntList, NbtLongList, NbtFloatList, NbtDoubleList {
	boolean add(V v);

	Iterator<V> iterator();

	Iterator<T> nbtIterator();

	R toRawArray();

	@Override
	default byte[] asBytesRaw() {
		return (byte[]) toRawArray();
	}

	@Override
	default int[] asIntsRaw() {
		return (int[]) toRawArray();
	}

	@Override
	default long[] asLongsRaw() {
		return (long[]) toRawArray();
	}
}
