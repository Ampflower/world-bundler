package gay.ampflower.bundler.compress;

import gay.ampflower.bundler.utils.Identifier;
import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.utils.Registry;
import gay.ampflower.bundler.world.region.McRegionHandler;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import org.slf4j.Logger;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class CompressorRegistry {
	private static final Logger logger = LogUtils.logger();

	public static final CompressorRegistry vanilla = new CompressorRegistry();
	final Int2ReferenceMap<Compressor> mcRegionCompressors = new Int2ReferenceOpenHashMap<>();
	final Reference2IntMap<Compressor> compressorToMcRegion = new Reference2IntOpenHashMap<>();
	final Registry<Compressor> customCompressors = new Registry<>();
	final Compressor[] fileCompressors = {
		GZipCompressor.INSTANCE,
		ZlibCompressor.INSTANCE,
		ZstdCompressor.INSTANCE,
		Lz4BlockCompressor.INSTANCE
	};

	static {
		vanilla.add(McRegionHandler.COMPRESSION_GZIP, "gzip", GZipCompressor.INSTANCE);
		vanilla.add(McRegionHandler.COMPRESSION_ZLIB, "zlib", ZlibCompressor.INSTANCE);
		vanilla.add(McRegionHandler.COMPRESSION_NONE, "none", NoneCompressor.INSTANCE);
		vanilla.add(McRegionHandler.COMPRESSION_LZ4, "lz4_block", Lz4BlockCompressor.INSTANCE);

		vanilla.add(-1, "zstd", ZstdCompressor.INSTANCE);

		// Intrude custom handler; it cannot exist in the registry.
		vanilla.mcRegionCompressors.put(McRegionHandler.COMPRESSION_CUSTOM, CustomCompressor.INSTANCE);
	}

	private void add(int mcRegion, String path, Compressor compressor) {
		try {
			assert compressor.deflater(OutputStream.nullOutputStream()) != null : path + ": deflater (" + compressor + ')';
			assert compressor.inflater(InputStream.nullInputStream()) != null : path + ": inflater (" + compressor + ')';
		} catch (EOFException eof) {
			logger.trace("{} ({}) threw an exception", path, compressor, eof);
		} catch (IOException ioe) {
			throw new AssertionError(ioe);
		}
		customCompressors.add(Identifier.ofBundler(path), compressor);
		if (mcRegion >= 0) {
			mcRegionCompressors.put(mcRegion, compressor);
		}
	}

	public Compressor getMcRegion(int id) {
		return mcRegionCompressors.get(id);
	}

	public int getMcRegionId(Compressor compressor) {
		return compressorToMcRegion.getInt(compressor);
	}

	public Compressor get(Identifier id) {
		return customCompressors.get(id);
	}

	public Identifier getId(Compressor compressor) {
		return customCompressors.getId(compressor);
	}
}
