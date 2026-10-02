package gay.ampflower.bundler;

import com.j256.simplemagic.ContentInfo;
import gay.ampflower.bundler.command.FindAndReplace;
import gay.ampflower.bundler.command.OpaqueBlobScraper;
import gay.ampflower.bundler.compress.Compressor;
import gay.ampflower.bundler.compress.CompressorRegistry;
import gay.ampflower.bundler.data.ini.Ini;
import gay.ampflower.bundler.recovery.Recovery;
import gay.ampflower.bundler.utils.EncodedStringMap;
import gay.ampflower.bundler.utils.Identifier;
import gay.ampflower.bundler.utils.LevelConverter;
import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.utils.MagicUtils;
import gay.ampflower.bundler.utils.SizeUtils;
import gay.ampflower.bundler.world.Region;
import gay.ampflower.bundler.world.region.LinearHandler;
import gay.ampflower.bundler.world.region.McRegionHandler;
import joptsimple.OptionParser;
import joptsimple.ValueConverter;
import joptsimple.util.EnumConverter;
import joptsimple.util.PathConverter;
import org.slf4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ScopeType;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

@Command(
	name = "world-bundler",
	version = "0.0.0",
	description = """
		Bundles your worlds so you don't have to.

		Various data-recovery and integrity tooling is also available.
		"""
)
public final class App {
	static {
		// Ensure that anyone that uses stdout will be forced to use stderr.
		// This is a compressor that is capable of shuttling stuff through I/O,
		// so anything being sent through other channels is inappropriate.
		System.setOut(System.err);
	}

	private static final Logger logger = LogUtils.logger();

	@Option(
		// Covers all the potential edgecases
		names = {"--help", "-Help", "-h", "-?", "/?"},
		usageHelp = true,
		description = "Displays this help message.",
		scope = ScopeType.INHERIT
	)
	private boolean helpRequested;

	public static void main(String[] args) throws IOException {
		final var cli = new CommandLine(new App());

		cli.registerConverter(Charset.class, Charset::forName);

		System.exit(cli.execute(args));

		if (true) {
			return;
		}

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

	@Command(description = "Simple string-based find to replace for Minecraft worlds.")
	public int findAndReplace(
		@Parameters(
			paramLabel = "<input>",
			index = "0",
			description = "The path to the Minecraft world."
		) final Path input,

		@Parameters(
			paramLabel = "<output>",
			index = "1",
			description = "Where the modified Minecraft world will be written."
		) final Path output,

		@Parameters(
			paramLabel = "<conversion>",
			index = "2",
			description = "The conversion specification, read as a Windows-spec INI file."
		) final Path conversion
	) throws IOException {
		// We need the full normalized absolute path for what we want.
		FindAndReplace.main(
			input.toAbsolutePath().normalize(),
			output.toAbsolutePath().normalize(),
			Ini.read(conversion, StandardCharsets.UTF_8)
		);

		return 0;
	}

	@Command(description = "Attempts to recover lost data from Minecraft worlds.")
	public int recover(
		@Parameters(
			paramLabel = "<input>",
			index = "0",
			description = "The path to any arbitrary world."
		) final Path input,

		@Parameters(
			paramLabel = "<output>",
			index = "1",
			description = "Where recovered data will be written."
		) final Path output
	) throws IOException {
		return Recovery.main(input, output);
	}

	@Command(description = "Scans given files for recoverable artifacts. Essentially PhotoRec at home.")
	public int scan(
		@Option(
			names = {"--sector-size", "-s", "/S"},
			paramLabel = "<size>",
			description = "The size of the sectors on disk.",
			showDefaultValue = Help.Visibility.ALWAYS,
			defaultValue = "512"
		) final int sectorSize,

		@Option(
			names = {"--find", "-f", "/F"},
			paramLabel = "<string>",
			description = "What strings to find within the given files. May be repeated for multiple search strings. Will increase the time required to complete."
		) final List<String> find,

		@Option(
			names = {"--charset", "-c", "/C"},
			paramLabel = "<charset>",
			description = "What charsets to scan with? May increase the time required to complete.",
			showDefaultValue = Help.Visibility.ALWAYS,
			defaultValue = "UTF-8"
		) final List<Charset> charsets,

		@Option(
			names = {"--paranoia", "-p", "/P"},
			description = "Whether to try scrape every byte offset for data. Will increase the time required to complete.",
			defaultValue = "false",
			negatable = true
		) final boolean paranoia,

		@Option(
			names = {"--output", "-o", "/O"},
			paramLabel = "<directory>",
			description = "Where to write the files.",
			showDefaultValue = Help.Visibility.ALWAYS,
			defaultValue = "."
		) final Path output,

		@Parameters(
			paramLabel = "<file>",
			description = "Files (including raw drives on UNIX-likes) to scrape for data.",
			arity = "1.."
		) final List<Path> files
	) throws Exception {
		return OpaqueBlobScraper.call(
			sectorSize,
			find.isEmpty() ? EncodedStringMap.NONE : new EncodedStringMap(charsets, find),
			paranoia,
			output.toAbsolutePath().normalize(),
			files
		);
	}

	// Test
	@Command(description = "Gives information as to what the file is.")
	public int file(
		@Parameters(
			paramLabel = "<path>",
			index = "0",
			description = "The path to test.",
			arity = "0..1" // can be omitted
		) final Path path
	) throws IOException {
		final InputStream stream;
		if (path == null) {
			stream = new FileInputStream(FileDescriptor.in);
		} else {
			if (!Files.isRegularFile(path)) {
				logger.warn("Not a regular file: {}", path);
				return 1;
			}

			stream = Files.newInputStream(path);
		}

		try (stream) {
			final byte[] bytes = stream.readNBytes(32 * (int) SizeUtils.KiB);

			final ContentInfo info = MagicUtils.test(bytes);

			if (info == null) {
				logger.warn("Could not detect file from {} bytes", bytes.length);
				return 2;
			}

			logger.info(
				"{}\nContent Type: {}\nName: {}\nMIME: {}",
				info.getMessage(),
				info.getContentType(),
				info.getName(),
				info.getMimeType()
			);
		}

		return 0;
	}
}
