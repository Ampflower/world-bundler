package gay.ampflower.bundler.world.io;

import gay.ampflower.bundler.world.Region;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Ampflower
 * @since ${version}
 **/
public interface RegionHandler {
	Region readRegion(InputStream stream) throws IOException;

	void writeRegion(OutputStream stream, Region region) throws IOException;
	default void writeRegion(OutputStream stream, Region region, ChunkWriter chunkWriter) throws IOException {
		writeRegion(stream, region);
	}
}
