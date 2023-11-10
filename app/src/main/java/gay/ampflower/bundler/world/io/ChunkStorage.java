package gay.ampflower.bundler.world.io;

import gay.ampflower.bundler.world.Region;

import java.io.IOException;

/**
 * @author Ampflower
 * @since ${version}
 **/
public interface ChunkStorage {
	byte[] readChunk(int x, int y) throws IOException;

	void writeChunk(int x, int y, byte[] chunk) throws IOException;

	Region readRegion(int x, int y) throws IOException;

	void writeRegion(int x, int y, Region region) throws IOException;

	boolean isNativelyRegioned();
}
