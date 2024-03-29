package gay.ampflower.bundler;

import gay.ampflower.bundler.compress.Compressor;
import gay.ampflower.bundler.compress.CompressorRegistry;
import gay.ampflower.bundler.utils.Identifier;
import gay.ampflower.bundler.utils.LevelConverter;
import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.world.Region;
import gay.ampflower.bundler.world.region.LinearHandler;
import gay.ampflower.bundler.world.region.McRegionHandler;
import joptsimple.OptionParser;
import joptsimple.ValueConverter;
import joptsimple.util.EnumConverter;
import joptsimple.util.PathConverter;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class App {
	private static final Logger logger = LogUtils.logger();

	static {
		// Ensure that anyone using stdout will be forced to use stderr.
		System.setOut(System.err);
	}

	public static void main(String[] args) throws IOException {

		final Path regionIn = Path.of(args[0]);
		final Path regionOut = Path.of(args[1]);

		final Region region;

		try (final var stream = Files.newInputStream(regionIn)) {
			region = McRegionHandler.INSTANCE.readRegion(0, 0, stream);
		}

		try (final var stream = Files.newOutputStream(regionOut, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			LinearHandler.INSTANCE.writeRegion(stream, region);
		}

		if(true) {
			return;
		}

		var parser = new OptionParser();

		// I/O specification
		var inputArgument = parser.acceptsAll(List.of("i", "input"), "Input file")
			.withRequiredArg().withValuesConvertedBy(new PathConverter());
		var outputArgument = parser.acceptsAll(List.of("o", "output"), "Output file")
			.withRequiredArg().withValuesConvertedBy(new PathConverter());

		// Compress and/or convert
		var decompressArgument = parser.acceptsAll(List.of("d", "decompress"), "Decompresses the input file.");
		var compressArgument = parser.acceptsAll(List.of("c", "compress"), "Compresses the input.");

		decompressArgument.availableUnless(compressArgument);
		compressArgument.availableUnless(decompressArgument);

		parser.acceptsAll(List.of("x", "convert"), "Converts the input into a new format.")
			.withRequiredArg().withValuesConvertedBy(new EnumConverter<>(LevelConverter.class) {});
		var compressorArgument = parser.accepts("compressor", "The compressor used for compressing.")
			.withRequiredArg().withValuesConvertedBy(new ValueConverter<Compressor>() {
				@Override
				public Compressor convert(final String value) {
					return CompressorRegistry.vanilla.get(Identifier.ofBundler(value));
				}

				@Override
				public Class<? extends Compressor> valueType() {
					return Compressor.class;
				}

				@Override
				public String valuePattern() {
					return null;
				}
			});

		// Misc
		var filterArgument = parser.acceptsAll(List.of("f", "filter"), "Filter the files to pack, unpack or convert.")
			.withRequiredArg().ofType(String.class);

		// Special case here is if X is specified, since that'd allow it to trim lighting data.
		var lossyArgument = parser.acceptsAll(List.of("l", "lossy"), "Whether to compress files in a lossy manner, trimming unneeded data.")
			.withOptionalArg().ofType(Boolean.class).defaultsTo(true);

		// Only applies when compressing.
		var lossyJarArgument = parser.accepts("lossyJar", "Whether to use Pack200 or reprocess jars.")
			.withOptionalArg().ofType(Boolean.class).defaultsTo(false);

		if(args.length == 0) {
			parser.printHelpOn(System.err);
			System.exit(1);
			return;
		}

		var options = parser.parse(args);

		var input = options.valueOf(inputArgument);
		var output = options.valueOf(outputArgument);
	}
}
