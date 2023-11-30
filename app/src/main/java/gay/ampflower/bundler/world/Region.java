package gay.ampflower.bundler.world;

import gay.ampflower.bundler.utils.ArrayUtils;

import java.util.Arrays;
import java.util.Objects;

/**
 * Raw region representation in memory.
 *
 * @author Ampflower
 * @since ${version}
 **/
public record Region(
	int x,
	int y,
	Chunk[] chunks
) {
	public static final int REGION_BOUND = 32;
	public static final int BIT_SHIFT = 5;
	public static final int BIT_MASK = 31;
	public static final int CHUNK_COUNT = REGION_BOUND * REGION_BOUND;

	public Region(int x, int y, int[] timestamps, byte[][] chunks) {
		this(x, y, toChunks(x, y, timestamps, chunks));
	}

	public Chunk getChunk(int x, int y) {
		return chunks[x * REGION_BOUND + y];
	}

	public void setChunk(int x, int y, Chunk chunk) {
		this.chunks[x * REGION_BOUND + y] = chunk;
	}

	public int size() {
		return ArrayUtils.sumNullable(chunks, Chunk::size);
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final Region region = (Region) o;
		return x == region.x && y == region.y && Arrays.equals(chunks, region.chunks);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(x, y);
		result = 31 * result + Arrays.hashCode(chunks);
		return result;
	}

	public static int getChunkX(int i) {
		return i & BIT_MASK;
	}

	public static int getChunkY(int i) {
		return i >> BIT_SHIFT;
	}


	public static int getChunkX(int regionX, int i) {
		return regionX * REGION_BOUND + getChunkX(i);
	}

	public static int getChunkY(int regionY, int i) {
		return regionY * REGION_BOUND + getChunkY(i);
	}

	private static Chunk[] toChunks(int x, int y, int[] timestamps, byte[][] arrays) {
		x *= REGION_BOUND;
		y *= REGION_BOUND;
		final var chunks = new Chunk[arrays.length];

		for (int i = 0; i < arrays.length; i++) {
			if (arrays[i] != null) {
				chunks[i] = new Chunk(x + getChunkX(i), y + getChunkY(i), timestamps[i], arrays[i]);
			}
		}

		return chunks;
	}
}
