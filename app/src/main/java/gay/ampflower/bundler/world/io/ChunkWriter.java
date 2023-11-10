package gay.ampflower.bundler.world.io;

import gay.ampflower.bundler.utils.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * @author Ampflower
 * @since ${version}
 **/
public interface ChunkWriter {
	void writeChunk(int i, byte[] data) throws IOException;


	public final class McLogger implements ChunkWriter {
		static final Logger logger = LogUtils.logger();
		@Override
		public void writeChunk(final int i, final byte[] data) {
			logger.info("{} -> {}", i, data);
		}
	}
}
