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
}
