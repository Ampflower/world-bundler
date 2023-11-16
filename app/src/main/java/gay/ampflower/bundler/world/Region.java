package gay.ampflower.bundler.world;

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
			chunks[i] = new Chunk(x + getChunkX(i), y + getChunkY(i), timestamps[i], arrays[i]);
		}

		return chunks;
	}
}
