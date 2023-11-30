package gay.ampflower.bundler.world;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Ampflower
 * @since ${version}
 **/
public record Chunk(
	int x,
	int y,
	int timestamp,
	byte[] array
) {

	public Chunk(int rx, int ry, int i, int timestamp, byte[] array) {
		this(Region.getChunkX(rx, i), Region.getChunkY(ry, i), timestamp, array);
	}

	public int size() {
		return array.length;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final Chunk chunk = (Chunk) o;
		return x == chunk.x && y == chunk.y && timestamp == chunk.timestamp && Arrays.equals(array, chunk.array);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(x, y, timestamp);
		result = 31 * result + Arrays.hashCode(array);
		return result;
	}
}
