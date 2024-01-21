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
	Chunk[] chunks,
	Object meta
) {
	public static final int REGION_BOUND = 32;
	public static final int BIT_SHIFT = 5;
	public static final int BIT_MASK = 31;
	public static final int CHUNK_COUNT = REGION_BOUND * REGION_BOUND;

	public Region {
		if (chunks.length != CHUNK_COUNT) throw new IllegalArgumentException("chunks[" + chunks.length + "]");
	}

	public Region(int x, int y, Chunk[] chunks) {
		this(x, y, chunks, null);
	}

	public Region(int x, int y, int[] timestamps, byte[][] chunks) {
		this(x, y, toChunks(x, y, timestamps, chunks));
	}

	public Region(int x, int y) {
		this(x, y, new Chunk[CHUNK_COUNT]);
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

	public boolean isEmpty() {
		for (final var chunk : chunks) {
			if (chunk != null && chunk.size() > 0) {
				return false;
			}
		}
		return true;
	}

	public boolean isFull() {
		for (final var chunk : chunks) {
			if (chunk == null || chunk.size() == 0) {
				return false;
			}
		}
		return true;
	}

	public int popCount() {
		int i = 0;
		for (final var chunk : chunks) {
			if (chunk != null && chunk.size() > 0) {
				i++;
			}
		}
		return i;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final Region region = (Region) o;
		return x == region.x
			&& y == region.y
			&& Objects.equals(meta, region.meta)
			&& chunkArraysEqual(chunks, region.chunks);
	}

	private static boolean chunkArraysEqual(final Chunk[] a, final Chunk[] b) {
		for (int i = 0; i < CHUNK_COUNT; i++) {
			if (!chunksEqual(a[i], b[i])) {
				return false;
			}
		}
		return true;
	}

	private static boolean chunksEqual(final Chunk a, final Chunk b) {
		boolean an = a == null || a.size() == 0;
		boolean bn = b == null || b.size() == 0;
		return (an & bn) || (!an && a.equals(b));
	}

	public Region pos(int x, int y) {
		return new Region(x, y, chunks, meta);
	}

	public Region meta(Object meta) {
		return new Region(x, y, chunks, meta);
	}

	@Override
	public int hashCode() {
		int result = Objects.hash(x, y, meta);
		result = 31 * result + Arrays.hashCode(chunks);
		return result;
	}

	@Override
	public String toString() {
		final var sb = new StringBuilder(1024).append("Region ").append(x).append(", ").append(y).append('\n');
		int n = 0;
		for (int i = 0; i < CHUNK_COUNT; i++) {
			final var chunk = chunks[i];
			if (chunk == null || chunk.size() == 0) {
				n++;
			} else {
				if (n > 0) {
					sb.append("null x").append(n).append('\n');
					n = 0;
				}
				sb.append(chunk).append('\n');
			}
		}
		if (n > 0) {
			sb.append("null x").append(n).append('\n');
		}
		if (meta instanceof Iterable<?> iterable) {
			sb.append("\nmeta:\n");
			for (final var i : iterable) {
				sb.append(i).append('\n');
			}
		} else if (meta instanceof Object[] array) {
			sb.append("\nmeta:\n");
			for (final var i : array) {
				sb.append(i).append('\n');
			}
		} else if (meta != null) {
			sb.append("\nmeta: ").append(meta).append('\n');
		}
		return sb.toString();
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
