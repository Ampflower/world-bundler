package gay.ampflower.bundler.command;

import gay.ampflower.bundler.compress.Compressor;
import gay.ampflower.bundler.compress.ZstdCompressor;
import gay.ampflower.bundler.data.ini.Ini;
import gay.ampflower.bundler.data.json.JsonUtil;
import gay.ampflower.bundler.data.json.io.FilteredJsonParser;
import gay.ampflower.bundler.data.json.io.JsonReader;
import gay.ampflower.bundler.data.json.io.JsonWriter;
import gay.ampflower.bundler.data.json.io.SaxJsonParser;
import gay.ampflower.bundler.nbt.io.NbtReader;
import gay.ampflower.bundler.nbt.io.NbtWriter;
import gay.ampflower.bundler.nbt.io.SaxFilteredNbtParser;
import gay.ampflower.bundler.nbt.io.SaxNbtParser;
import gay.ampflower.bundler.nbt.io.SaxNbtReader;
import gay.ampflower.bundler.nbt.io.SaxTreeWriter;
import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.utils.SysProps;
import gay.ampflower.bundler.utils.pos.Pos2i;
import gay.ampflower.bundler.world.Chunk;
import gay.ampflower.bundler.world.Region;
import gay.ampflower.bundler.world.io.dir.RegionData;
import gay.ampflower.bundler.world.io.dir.Visitors;
import org.slf4j.Logger;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class FindAndReplace {
	private static final boolean debug = SysProps.isDebuggee();

	private static final Logger logger = LogUtils.logger();

	public static void main(
		final Path inputPath,
		final Path outputPath,
		final Ini conversion
	) throws IOException {
		logger.info("Discovering {}...", inputPath);

		// TODO: this is insufficient for a copy & convert
		final var meta = Visitors.walkMinecraft(inputPath);

		logger.info(
			"Resolved {} directories with {} files. Points of interest: {}",
			meta.dirs(),
			meta.files(),
			meta.poi().keySet()
		);

		if (meta.poi().isEmpty()) {
			logger.error("No world roots found.");
			System.exit(2);
			return;
		}

		logger.info("dimensions -> {}", meta.poi().get("dimensions"));

		final var untree = new IdentityHashMap<RegionData, String>();
		final var tree = new HashMap<String, RegionData>();

		for (final var dir : meta.visited()) {
			final var rel = inputPath.relativize(dir.dir).toString();
			tree.put(rel, dir);
			untree.put(dir, rel);
			logger.debug("{} => {}", rel, dir.dir);
		}

		final var exec = Executors.newWorkStealingPool(Runtime.getRuntime().availableProcessors());

		for (final var poi : meta.visited()) {
			logger.debug("Found: {}", poi);

			final var outputPoi = retarget(inputPath, outputPath, poi.dir);
			try {
				Files.createDirectories(outputPoi);
			} catch (IOException e) {
				logger.warn("Unable to create `{}`; any transform is lossy from here.", outputPoi, e);
				continue;
			}

			for (final var entry : poi.regions.entrySet()) {
				final var resolver = entry.getKey();
				final var values = entry.getValue();

				logger.info("Found {} {} regions", values.size(), resolver);

				exec.execute(() -> {

					final var inputStorage = resolver.createChunkStorage(poi.dir);
					final var outputStorage = resolver.createChunkStorage(outputPoi);
					final var itr = values.iterator();

					while (itr.hasNext()) {
						final long value = itr.nextLong();
						final int x = Pos2i.x(value), y = Pos2i.y(value);
						Region region = null;

						try {
							region = inputStorage.readRegion(x, y);
						} catch (IOException ioe) {
							logger.warn("Unable to read region {}, {} @ {}:", x, y, poi.dir, ioe);
						}

						if (region == null) {
							logger.trace("Region {}, {} @ {} is missing?", x, y, poi.dir);
							continue;
						}

						if (region.isEmpty()) {
							logger.trace("Region {}, {} @ {} is empty?", x, y, poi.dir);
							continue;
						}

						logger.trace("Read region {}, {} @ {}:\n{}", x, y, poi.dir, region);

						for (int i = 0; i < region.chunks().length; i++) {
							final var chunk = region.chunks()[i];

							if (chunk == null || chunk.isEmpty()) {
								continue;
							}

							final var writer = new SaxTreeWriter();
							final var filtered = new ReplacingNbtParser(writer, conversion);

							try {
								filtered.push(chunk.nbt());
							} catch (IOException ioException) {
								throw new AssertionError("Wait, what? The I/O managed to exist.", ioException);
							}

							region.chunks()[i] = new Chunk(chunk, writer.getRoot());
						}

						try {
							logger.trace("Saving region {}, {} @ {}:\n{}", x, y, outputPoi, region);
							outputStorage.writeRegion(x, y, region);
						} catch (IOException ioe) {
							logger.warn("Unable to write region {}, {} @ {}\nRegion: {}", x, y, outputPoi, region, ioe);
						}
					}
				});
			}

			for (final var entry : poi.paths) {

				if (process(inputPath, outputPath, entry, conversion)) {
					continue;
				}

				if (relink(inputPath, outputPath, entry)) {
					continue;
				}

				final var target = retarget(inputPath, outputPath, entry);

				Files.copy(entry, target, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
			}
		}

		logger.info("Finished, awaiting shutdown of {}", exec);

		exec.close();
		logger.info("Completed.");
	}

	private static Path retarget(final Path inputPath, final Path outputPath, final Path path) {
		final var normalized = path.normalize();
		final var relativized = inputPath.relativize(normalized);
		return outputPath.resolve(relativized);
	}

	private static boolean relink(final Path inputPath, final Path outputPath, final Path link) throws IOException {
		if (!Files.isSymbolicLink(link)) {
			return false;
		}

		final Path target = Files.readSymbolicLink(link);
		final Path resolved;

		if (!target.isAbsolute()) {
			resolved = link.getParent().resolve(target).normalize();
		} else {
			resolved = target.normalize();
		}

		if (!resolved.startsWith(inputPath)) {
			logger.warn(
				"Cowardly refusing to relink \"{}\" as the target \"{}\" is outside of \"{}\". Copying instead.",
				link,
				target,
				inputPath
			);
			return false;
		}

		final Path newLink = retarget(inputPath, outputPath, link);

		Files.createSymbolicLink(newLink, link.relativize(resolved));

		return true;
	}

	/**
	 * Processes .dat, .zat and .json files
	 */
	private static boolean process(
		final Path inputPath,
		final Path outputPath,
		final Path data,
		final Ini conversion
	) throws IOException {
		final String fileName = data.getFileName().toString();
		final int extIndex = fileName.lastIndexOf('.');
		if (extIndex < 0) {
			return false;
		}

		final String ext = fileName.substring(extIndex + 1);

		final Compressor compressor;
		final byte[] nbt;

		switch (ext) {
			case "json" -> {
				final var output = retarget(inputPath, outputPath, data);

				try (
					final var outputWriter = Files.newBufferedWriter(output, StandardCharsets.UTF_8);
					final var jsonWriter = new JsonWriter(outputWriter);

					final var inputReader = Files.newBufferedReader(data, StandardCharsets.UTF_8);
					final var jsonReader = new JsonReader(inputReader)
				) {
					logger.debug("Processing {} => {}", data, output);
					jsonReader.parse(new ReplacingJsonParser(jsonWriter, conversion));
				}

				return true;
			}
			case "dat", "schem" -> {
				final var bytes = Files.readAllBytes(data);
				compressor = Compressor.getFileCompressor(bytes);
				nbt = compressor.inflate(bytes);
			}
			case "zat" -> {
				// Fireblanket-specific extension
				compressor = ZstdCompressor.INSTANCE;
				nbt = ZstdCompressor.INSTANCE.inflate(Files.readAllBytes(data));
			}
			default -> {
				return false;
			}
		}

		final var output = retarget(inputPath, outputPath, data);

		try (
			final var outputStream = Files.newOutputStream(
				output,
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING
			);
			final var outputCompressor = compressor.deflater(outputStream);
			final var outputBuffer = new BufferedOutputStream(outputCompressor);
			final var nbtWriter = new NbtWriter(outputBuffer);

			final var inputStream = new ByteArrayInputStream(nbt);
			final var nbtReader = new NbtReader(inputStream)
		) {
			logger.debug("Processing {} => {}", data, output);
			SaxNbtReader.parse(new ReplacingNbtParser(nbtWriter, conversion), nbtReader);
		}


		return true;
	}

	private static class ReplacingNbtParser extends SaxFilteredNbtParser {
		private final Ini conversion;
		private final Map<String, String> header;
		private String witnessField;

		public ReplacingNbtParser(final SaxNbtParser parser, final Ini conversion) {
			super(parser);
			this.conversion = conversion;
			this.header = conversion.getSection(null);
		}

		@Override
		public void field(final String name) throws IOException {
			this.debug(name);
			this.witnessField = name;
			super.field(header.getOrDefault(name, name));
		}

		@Override
		public void ofString(final String value) throws IOException {
			{
				final var replacement = header.getOrDefault(value, value);
				if (replacement != value) {
					logger.debug("Encountered {}", value);
					super.ofString(replacement);
					return;
				}
			}

			if (!JsonUtil.isProbablyJson(value)) {
				super.ofString(value);
				return;
			}

			final var replacement = new StringWriter();
			try (
				replacement;
				final var jsonWriter = new JsonWriter(replacement);

				final var reader = new StringReader(value);
				final var jsonReader = new JsonReader(reader)
			) {
				jsonReader.parse(new ReplacingJsonParser(jsonWriter, conversion));
			} catch (IOException ioException) {
				logger.debug("Not JSON: {}? -> {}", witnessField, value, ioException);
				super.ofString(value);
				return;
			}
			super.ofString(replacement.toString());
		}

		private void debug(final String value) {
			if (debug && header.containsKey(value)) {
				logger.debug("Encountered {}", value);
			}
		}
	}

	private static class ReplacingJsonParser extends FilteredJsonParser {
		private final Map<String, String> header;

		public ReplacingJsonParser(final SaxJsonParser parser, final Ini conversion) {
			super(parser);
			header = conversion.getSection(null);
		}

		@Override
		public void field(final String name) throws IOException {
			this.debug(name);
			super.field(header.getOrDefault(name, name));
		}

		@Override
		public void ofString(final String value) throws IOException {
			this.debug(value);
			super.ofString(header.getOrDefault(value, value));
		}

		private void debug(final String value) {
			if (debug && header.containsKey(value)) {
				logger.debug("Encountered {}", value);
			}
		}
	}
}
