package gay.ampflower.bundler.world;

/**
 * Raw region representation in memory.
 *
 * @author Ampflower
 * @since ${version}
 **/
public record Region(
	int[] timestamps,
	byte[][] chunks
) {
	public static final int REGION_BOUND = 32;
	public static final int BIT_SHIFT = 5;
	public static final int BIT_MASK = 31;
	public static final int CHUNK_COUNT = REGION_BOUND * REGION_BOUND;

	public Region() {
		this(
			new int[CHUNK_COUNT],
			new byte[CHUNK_COUNT][]
		);
	}

	public byte[] getChunk(int x, int y) {
		return chunks[x * REGION_BOUND + y];
	}

	public void setChunk(int x, int y, byte[] chunk) {
		this.chunks[x * REGION_BOUND + y] = chunk;
	}
}
