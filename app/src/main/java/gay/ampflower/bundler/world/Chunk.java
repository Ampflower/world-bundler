package gay.ampflower.bundler.world;

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
}
