package gay.ampflower.bundler.command;

import gay.ampflower.bundler.compress.Compressor;
import gay.ampflower.bundler.compress.CompressorRegistry;
import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.utils.SizeUtils;
import gay.ampflower.bundler.utils.SqlUtils;
import gay.ampflower.bundler.utils.io.n.ChannelInputStream;
import org.slf4j.Logger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Also known as PhotoRec at home
 *
 * @author Ampflower
 * @since ${version}
 **/
@Command(name = "scan", description = "Scans given files for recoverable artifacts.")
public final class OpaqueBlobScraper implements Callable<Integer> {
	private static final Logger logger = LogUtils.logger();

	/*
	 * TODO:
	 *  - SQLite3-backed indices
	 *    - This would allow resuming of the scraper in the event of power failure.
	 *    - A periodic backup of this database should be taken every 15 minutes.
	 *  - Compressor Sensor
	 *    - We will need to store indices of:
	 *      - Offset
	 *      - Sector
	 *    - We will need to find:
	 *      - ZStandard
	 *  - Supercluster, Superblock and other Filesystem Datastructures Sensor, of the following filesystems:
	 *    - ZFS
	 */

	@Option(names = "--sector-size", description = "The size of the sectors on disk. Defaults to 512.")
	int sectorSize = 512;

	@Option(
		names = {"--output", "-o"},
		description = "Where to write the files. Defaults to the current working directory."
	)
	Path output = Path.of(".");

	@Parameters(
		description = """
			Files (including raw drives on UNIX-likes) to scrape for data.
			"""
	)
	List<Path> files;

	@Override
	public Integer call() throws Exception {
		output = output.toAbsolutePath().normalize();

		Files.createDirectories(output);

		logger.info("Selected {} as the output path.", output);

		for (final var file : files) {
			if (Files.notExists(file)) {
				logger.warn("File not found: {}", file);
				continue;
			}

			if (Files.isDirectory(file)) {
				logger.warn("Not a file: {}", file);
				continue;
			}

			final var fileName = file.getFileName();

			if (fileName == null) {
				logger.warn("... nameless? {}", file);
				continue;
			}

			try (
				final var channel = FileChannel.open(file, StandardOpenOption.READ);
				// Retain a read-lock for the duration of the operation.
				// This *should* ideally prevent any competing applications from interfering.
				final var lock = channel.lock(0, Long.MAX_VALUE, true);
				final Connection connection = bootstrapDatabase(this.output, fileName)
			) {
				final long witnessSize = channel.size();

				final long expectedSize;
				final long sectorIndex;
				final int sectorSize;

				// TODO: Alias files for the database driver.
				try (
					final PreparedStatement metadataFetch = connection.prepareStatement("SELECT * FROM meta");
					final PreparedStatement metadataInsert = connection.prepareStatement(
						"INSERT INTO meta (input, sectorSize, expectedSize) VALUES (?, ?, ?)");
					final ResultSet results = metadataFetch.executeQuery()
				) {
					if (!results.next()) {
						expectedSize = witnessSize;
						sectorSize = this.sectorSize;
						sectorIndex = 0;

						metadataInsert.setString(1, file.toString());
						metadataInsert.setInt(2, sectorSize);
						metadataInsert.setLong(3, expectedSize);
						metadataInsert.execute();

						logger.debug(
							"Starting {} with sector size {} and expected size of {}",
							file,
							sectorSize,
							SizeUtils.displaySize(witnessSize)
						);
					} else {
						final String input = results.getString("input");
						expectedSize = results.getLong("expectedSize");
						sectorIndex = results.getLong("lastSector");
						sectorSize = results.getInt("sectorSize");

						logger.debug(
							"Resuming {} from sector {} (size: {}). Expecting {} at {}",
							file,
							sectorIndex,
							sectorSize,
							SizeUtils.displaySize(expectedSize),
							input
						);
					}
				}

				if (sectorIndex * sectorSize >= expectedSize) {
					logger.info("{} was already completed according to the database.", file);
					continue;
				}

				if (expectedSize != witnessSize) {
					throw new IOException("Expected " + expectedSize + ", got " + witnessSize);
				}

				scrape(connection, channel, file.toString(), sectorSize, sectorIndex);
			} catch (OverlappingFileLockException fae) {
				logger.warn("Competing lock for {}:", file, fae);
				// TODO: should we have a lock dumper?
			} catch (IOException ioe) {
				logger.warn("Unable to read {}:", file, ioe);
			} catch (SQLException sql) {
				logger.warn("SQL database failure for {}", file, sql);
			}
		}

		return 0;
	}

	private static Connection bootstrapDatabase(
		final Path output,
		final Path file
	) throws SQLException {
		final Path database = output.resolve("RECOVERY-" + file.getFileName() + ".db");

		logger.info("Selected {} as the database path for {}.", database, file);

		final Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);

		try (final var statement = connection.createStatement()) {
			// Carefully, we're picking as inter-compatible of types as possible that strictly define the
			// datastructures we expect. SQLite doesn't care, but I do.
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS meta(
					input TEXT NOT NULL,
					sectorSize INTEGER NOT NULL,
					lastSector BIGINT DEFAULT 0 NOT NULL,
					expectedSize BIGINT NOT NULL,
					stage TEXT NULL
				)
				""");
			// We will use the raw position for where stuff is located, least, initially.
			// NOTE: position SHOULD be BIGINT in standard SQL.
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS found(
					position INTEGER PRIMARY KEY,
					sample TEXT NULL
				)
				""");
			// Sectored system detecting for detecting all compressor streams.
			// NOTE: position SHOULD be BIGINT in standard SQL.
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS streams(
					position INTEGER PRIMARY KEY,
					sector BIGINT NOT NULL,
					offset INTEGER NOT NULL,
					type TEXT NOT NULL
				)
				""");
		}

		return connection;
	}

	private static void scrape(
		final Connection connection,
		final FileChannel channel,
		final String fileName,
		final int sectorSize,
		final long sectorIndex
	) throws SQLException, IOException {

		final PreparedStatement metadataUpdate = connection.prepareStatement(
			"UPDATE meta SET lastSector = ?");
		final PreparedStatement foundInsert = connection.prepareStatement(
			"INSERT OR IGNORE INTO found (position, sample) VALUES (?, ?)");
		final PreparedStatement streamInsert = connection.prepareStatement(
			"INSERT OR IGNORE INTO streams (position, sector, offset, type) VALUES (?, ?, ?, ?)");

		// TODO: retain a filesystem lock before establishing a database?
		final byte[] raw = new byte[sectorSize];
		final ByteBuffer buf = ByteBuffer.wrap(raw);
		final long size = channel.size();
		final long sectors = size / sectorSize;
		final long trailer = size % sectorSize;

		final long saveSectors = SizeUtils.GiB / sectorSize;
		// Save every gigabyte seeked
		long nextSaveSector = saveSectors;
		// Save every minute
		long nextSaveTime = System.currentTimeMillis() + (60 * 60 * 1000);

		if (trailer != 0L) {
			logger.warn(
				"Not a clean division in {}, final sector is {} bytes long. Data here might be incomplete or ignored.",
				fileName,
				trailer
			);
		}

		for (long i = sectorIndex; i < sectors; i++) {
			final long position = i * sectorSize;
			buf.clear();
			channel.read(buf, position);
			final Compressor compressor = Compressor.getFileCompressor(raw);
			final String safeSample;

			if (!compressor.isCompressor()) {
				logger.trace("Not a compressor block at {}", position);
				safeSample = "";
				if (System.currentTimeMillis() < nextSaveTime && i < nextSaveSector) {
					continue;
				}
			} else {
				final var output = new ByteArrayOutputStream();
				try (
					final var binput = ChannelInputStream.ofOffset(channel, position);
					final var cinput = compressor.inflater(binput)
				) {
					cinput.transferTo(output);
				} catch (Throwable t) {
					logger.trace("Swallowing, likely EOF", t);
				}

				if (output.size() != 0) {
					final byte[] bytes = output.toByteArray();

					final String sample = new String(bytes, 0, Math.min(bytes.length, 64), StandardCharsets.UTF_8);
					safeSample = URLEncoder.encode(sample, StandardCharsets.UTF_8);

					logger.debug(
						"Captured {} bytes from sector {} ({}), sample: {}",
						output.size(),
						i,
						position,
						safeSample
					);
				} else {
					safeSample = null;
				}
			}

			// TODO: scour sector for rogue compressor headers
			//  Should we use an entropy encoder like zxcvbn to narrow down which sectors to scan?
			//  High-entropy sectors are far more likely to have successive compression frames.
			// TODO: Any entropy encoder should be bypassed with --paranoia
			// TODO: Any candidate headers (included deflate) should be included on --paranoia

			final long currentSector = i;
			SqlUtils.transaction(connection, $ -> {
				metadataUpdate.setLong(1, currentSector);
				metadataUpdate.execute();

				if (!compressor.isCompressor() || safeSample == null) {
					return;
				}

				foundInsert.setLong(1, position);
				foundInsert.setString(2, safeSample);
				foundInsert.execute();

				streamInsert.setLong(1, position);
				streamInsert.setLong(2, currentSector);
				streamInsert.setInt(3, 0);
				streamInsert.setString(4, CompressorRegistry.vanilla.getId(compressor).toString());
				streamInsert.execute();
			});
			nextSaveTime = System.currentTimeMillis() + (60 * 60 * 1000);
			nextSaveSector = currentSector + saveSectors;
		}

		SqlUtils.transaction(connection, $ -> {
			metadataUpdate.setLong(1, sectors);
			metadataUpdate.execute();
		});
	}
}
