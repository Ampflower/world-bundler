package gay.ampflower.bundler.world;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;

/**
 * @author Ampflower
 * @since ${version}
 **/
public record Chunk(
	int x,
	int y,
	int timestamp,
	byte[] array,
	Object meta
) {

	public Chunk {
		Objects.requireNonNull(array, "array");
	}

	public Chunk(int x, int y, int timestamp, byte[] array) {
		this(x, y, timestamp, array, null);
	}

	public Chunk(int rx, int ry, int i, int timestamp, byte[] array) {
		this(Region.getChunkX(rx, i), Region.getChunkY(ry, i), timestamp, array);
	}

	public int size() {
		return array.length;
	}

	public Chunk pos(int x, int y) {
		return new Chunk(x, y, timestamp, array, meta);
	}

	public Chunk meta(Object meta) {
		return new Chunk(x, y, timestamp, array, meta);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final Chunk chunk = (Chunk) o;
		return x == chunk.x
			&& y == chunk.y
			&& timestamp == chunk.timestamp
			&& Objects.equals(meta, chunk.meta)
			&& Arrays.equals(array, chunk.array);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(x, y, timestamp, meta);
		result = 31 * result + Arrays.hashCode(array);
		return result;
	}

	@Override
	public String toString() {
		if (meta != null) {
			return x + ", " + y + " @ " + timestamp + " + " + meta + ": " + HexFormat.of().formatHex(this.array);
		}
		return x + ", " + y + " @ " + timestamp + ": " + HexFormat.of().formatHex(this.array);
	}
}
