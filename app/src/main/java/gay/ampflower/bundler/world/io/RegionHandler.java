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

	default Region readRegion(InputStream stream, ChunkReader chunkReader) throws IOException {
		return readRegion(stream);
	}

	void writeRegion(OutputStream stream, Region region) throws IOException;

	default void writeRegion(OutputStream stream, Region region, ChunkWriter chunkWriter) throws IOException {
		writeRegion(stream, region);
	}
}
