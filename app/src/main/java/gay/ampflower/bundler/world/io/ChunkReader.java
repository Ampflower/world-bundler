package gay.ampflower.bundler.world.io;

import gay.ampflower.bundler.utils.LevelCompressor;
import gay.ampflower.bundler.utils.LogUtils;
import org.slf4j.Logger;

/**
 * @author Ampflower
 * @since ${version}
 **/
public interface ChunkReader {
	byte[] readChunk(int i, LevelCompressor compressor);


	public final class McLogger implements ChunkReader {
		static final Logger logger = LogUtils.logger();

		@Override
		public byte[] readChunk(final int i, final LevelCompressor compressor) {
			logger.info("{} + {}", i, compressor);
			return null;
		}
	}
}
