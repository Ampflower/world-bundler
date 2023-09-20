package gay.ampflower.bundler.utils;

import gay.ampflower.bundler.world.McRegionHandler;

/**
 * @author Ampflower
 * @since ${version}
 **/
public enum LevelCompressor {
	NONE(McRegionHandler.COMPRESSION_NONE),
	GZIP(McRegionHandler.COMPRESSION_GZIP),
	ZLIB(McRegionHandler.COMPRESSION_ZLIB),
	ZSTD(-1),
	;

	public final byte MCREGION_TYPE;

	LevelCompressor(int mcRegionType) {
		this.MCREGION_TYPE = (byte) mcRegionType;
	}
}
