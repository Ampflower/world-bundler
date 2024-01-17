package gay.ampflower.bundler.world.io;

import gay.ampflower.bundler.utils.pos.Pos2i;
import gay.ampflower.bundler.world.Chunk;
import gay.ampflower.bundler.world.Region;

import java.io.IOException;
import java.util.Iterator;

/**
 * @author Ampflower
 * @since ${version}
 **/
public interface ChunkStorage {
	Chunk readChunk(int x, int y) throws IOException;

	void writeChunk(int x, int y, Chunk chunk) throws IOException;

	Region readRegion(int x, int y) throws IOException;

	void writeRegion(int x, int y, Region region) throws IOException;

	Iterator<Pos2i> iterateRegionCoords() throws IOException;

	Iterator<Region> iterateRegions() throws IOException;

	boolean isNativelyRegioned();
}
