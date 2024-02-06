package gay.ampflower.bundler.utils;

import java.util.*;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class Registry<V> {
	private final Map<Identifier, V> idToValue = new HashMap<>();
	private final Map<V, Identifier> valueToId = new IdentityHashMap<>();

	private final Map<V, Set<Identifier>> valueToAliases = new IdentityHashMap<>();

	public Registry() {
	}

	private void checkValueToId(final Identifier id, final V value) {
		final var oldId = valueToId.get(value);
		if (oldId != null) {
			throw new RuntimeException("Attempted to double-add " + value + " to " + id + "; already registered to " + oldId);
		}
	}

	private void checkIdToValue(final Identifier id, final V value) {
		final V oldValue = idToValue.get(id);
		if (oldValue != null) {
			throw new RuntimeException("Attempted to double-add " + id + " with " + value + "; already registered to " + oldValue);
		}
	}

	public V add(Identifier id, V value) {
		checkValueToId(id, value);
		checkIdToValue(id, value);

		idToValue.put(id, value);
		valueToId.put(value, id);

		return value;
	}

	public V alias(Identifier id, V value) {
		if (!valueToId.containsKey(value)) {
			return add(id, value);
		}

		checkIdToValue(id, value);

		idToValue.put(id, value);
		valueToAliases.computeIfAbsent(value, v -> new HashSet<>()).add(id);

		return value;
	}

	public V set(Identifier id, V value) {
		checkValueToId(id, value);

		final var oldValue = idToValue.put(id, value);
		valueToId.remove(oldValue);
		valueToId.put(value, id);

		return oldValue;
	}

	public V get(Identifier id) {
		return idToValue.get(id);
	}

	public Identifier getId(V value) {
		return valueToId.get(value);
	}

	public Set<Identifier> keys() {
		return idToValue.keySet();
	}

	public Set<V> values() {
		return valueToId.keySet();
	}
}
