package gay.ampflower.bundler.utils;

/**
 * @author Ampflower
 * @since ${version}
 **/
public enum LevelCompressor {
	NONE(0),
	GZIP(1),
	ZLIB(2),
	ZSTD(-1),
	;

	public final byte MCREGION_TYPE;

	LevelCompressor(int mcRegionType) {
		this.MCREGION_TYPE = (byte) mcRegionType;
	}
}
