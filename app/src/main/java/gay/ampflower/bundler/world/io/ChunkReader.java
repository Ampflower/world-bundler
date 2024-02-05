package gay.ampflower.bundler.world.io;

import gay.ampflower.bundler.compress.Compressor;
import gay.ampflower.bundler.utils.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * @author Ampflower
 * @since ${version}
 **/
public interface ChunkReader {
	byte[] readChunk(int i, Compressor compressor) throws IOException;


	public final class McLogger implements ChunkReader {
		static final Logger logger = LogUtils.logger();

		@Override
		public byte[] readChunk(final int i, final Compressor compressor) {
			logger.info("{} + {}", i, compressor);
			return null;
		}
	}
}
