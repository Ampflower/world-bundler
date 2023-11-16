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
	Region readRegion(final int x, final int y, InputStream stream) throws IOException;

	default Region readRegion(final int x, final int y, InputStream stream, ChunkReader chunkReader) throws IOException {
		return readRegion(x, y, stream);
	}

	void writeRegion(OutputStream stream, Region region) throws IOException;

	default void writeRegion(OutputStream stream, Region region, ChunkWriter chunkWriter) throws IOException {
		writeRegion(stream, region);
	}
}
