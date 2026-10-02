package gay.ampflower.bundler.command;

import com.j256.simplemagic.ContentInfo;
import gay.ampflower.bundler.compress.Compressor;
import gay.ampflower.bundler.compress.CompressorRegistry;
import gay.ampflower.bundler.utils.EncodedStringMap;
import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.utils.MagicUtils;
import gay.ampflower.bundler.utils.SizeUtils;
import gay.ampflower.bundler.utils.SqlUtils;
import gay.ampflower.bundler.utils.io.MemoryInputStream;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.net.URLEncoder;
import java.nio.ByteOrder;
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
import java.util.Objects;

/**
 * Also known as PhotoRec at home
 *
 * @author Ampflower
 * @since ${version}
 **/
public final class OpaqueBlobScraper {
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

	public static int call(
		final int scrapeSectorSize,
		final EncodedStringMap find,
		final boolean paranoia,
		final Path output,
		final List<Path> files
	) throws Exception {
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
				final var arena = Arena.ofShared();
				final var channel = FileChannel.open(file, StandardOpenOption.READ);
				// Retain a read-lock for the duration of the operation.
				// This *should* ideally prevent any competing applications from interfering.
				final var _ = channel.lock(0, Long.MAX_VALUE, true);
				final Connection connection = bootstrapDatabase(output, fileName)
			) {
				final long witnessSize = channel.size();

				final long expectedSize;
				final long sectorIndex;
				final int sectorSize;
				final long lastDataSector;
				final long lastEmptySector;

				// TODO: Alias files for the database driver.
				try (
					final PreparedStatement metadataFetch = connection.prepareStatement("SELECT * FROM meta");
					final PreparedStatement metadataInsert = connection.prepareStatement(
						"INSERT INTO meta (input, sectorSize, expectedSize) VALUES (?, ?, ?)");
					final ResultSet results = metadataFetch.executeQuery()
				) {
					if (!results.next()) {
						expectedSize = witnessSize;
						sectorSize = scrapeSectorSize;
						sectorIndex = 0;
						lastDataSector = -1;
						lastEmptySector = -1;

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
						lastDataSector = results.getLong("lastDataSector");
						lastEmptySector = results.getLong("lastEmptySector");

						logger.debug(
							"Resuming {} from sector {} (size: {}). Expecting {} at {}. Last data sector: {}. Last empty sector: {}",
							file,
							sectorIndex,
							sectorSize,
							SizeUtils.displaySize(expectedSize),
							input,
							lastDataSector,
							lastEmptySector
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

				final MemorySegment segment = channel.map(FileChannel.MapMode.READ_ONLY, 0, witnessSize, arena);

				scrape(
					connection,
					segment,
					file.toString(),
					sectorSize,
					sectorIndex,
					lastDataSector,
					lastEmptySector,
					find,
					paranoia
				);
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
					lastDataSector BIGINT DEFAULT -1 NOT NULL,
					lastEmptySector BIGINT DEFAULT -1 NOT NULL,
					expectedSize BIGINT NOT NULL,
					stage TEXT NULL
				)
				""");
			// Sectored system detecting for detecting all compressor streams.
			// The MIME is separate to allow it to tell the contents of compressed files.
			// NOTE: position SHOULD be BIGINT in standard SQL.
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS streams(
					position INTEGER PRIMARY KEY,
					compressor TEXT NULL,
					mime TEXT NULL
				)
				""");
			// We will use the raw position for where stuff is located, least, initially.
			// NOTE: position SHOULD be BIGINT in standard SQL.
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS found(
					position INTEGER PRIMARY KEY,
					sample TEXT NULL,
					FOREIGN KEY (position) REFERENCES streams (position) ON DELETE CASCADE
				)
				""");
			// Has data at all. This will allow quicker rescans in the future.
			// NOTE: sector SHOULD be BIGINT in standard SQL.
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS data(
					sector INTEGER PRIMARY KEY,
					length BIGINT NOT NULL
				)
				""");
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS strings(
					id INTEGER PRIMARY KEY,
					content TEXT NOT NULL UNIQUE
				)
				""");
			// Used for find indexing. i.e. paranoia mode.
			// NOTE: position SHOULD be BIGINT in standard SQL.
			// offset will be used for compressed streams.
			statement.executeUpdate("""
				CREATE TABLE IF NOT EXISTS poi(
					position INTEGER PRIMARY KEY,
					offset BIGINT NOT NULL,
					interest INTEGER NOT NULL,
					FOREIGN KEY (interest) REFERENCES strings (id) ON DELETE CASCADE
				)
				""");
		}

		return connection;
	}

	private static void scrape(
		final Connection connection,
		final MemorySegment memory,
		final String fileName,
		final int sectorSize,
		final long sectorIndex,
		// intentionally mutable
		long dataSector,
		// intentionally mutable
		long emptySector,
		final EncodedStringMap find,
		final boolean paranoia
	) throws SQLException, IOException {

		final PreparedStatement metadataUpdate = connection.prepareStatement(
			"UPDATE meta SET lastSector = ?, lastDataSector = ?, lastEmptySector = ?");
		final PreparedStatement foundInsert = connection.prepareStatement(
			"INSERT OR IGNORE INTO found (position, sample) VALUES (?, ?)");
		final PreparedStatement streamInsert = connection.prepareStatement(
			"INSERT OR IGNORE INTO streams (position, compressor, mime) VALUES (?, ?, ?)");
		final PreparedStatement dataInsert = connection.prepareStatement(
			"INSERT OR IGNORE INTO data (sector, length) VALUES (?, ?)");
		final PreparedStatement stringInsert = connection.prepareStatement(
			"INSERT OR IGNORE INTO strings (content) VALUES (?) RETURNING id");
		final PreparedStatement stringSelect = connection.prepareStatement(
			"SELECT id FROM strings WHERE content = ?");
		final PreparedStatement poiInsert = connection.prepareStatement(
			"INSERT INTO poi (position, offset, interest) VALUES (?, ?, ?) ON CONFLICT (position) DO NOTHING");

		// TODO: retain a filesystem lock before establishing a database?
		final byte[] raw = new byte[sectorSize];
		final long size = memory.byteSize();
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
			final int remaining = (position + sectorSize) > size ? (int) (size % sectorSize) : sectorSize;
			MemorySegment.copy(memory, ValueLayout.JAVA_BYTE, position, raw, 0, remaining);

			if (bitOr(raw) == 0L) {
				if (dataSector > emptySector) {
					dataInsert.setLong(1, emptySector + 1L);
					dataInsert.setLong(2, i - emptySector);
					dataInsert.addBatch();
				}
				emptySector = i;
				continue;
			} else {
				dataSector = i;
			}

			// TODO: make the various compressors tolerant to errors
			final Compressor compressor = Compressor.getFileCompressor(raw);
			final String safeSample;
			final ContentInfo mime;
			final Long2ObjectMap<EncodedStringMap.Result> results;

			if (!compressor.isCompressor()) {
				logger.trace("Not a compressor block at {}", position);

				safeSample = null;
				mime = MagicUtils.test(memory, position);
				results = find.scan(memory, position, sectorSize);

				if (
					mime == null &&
					results.isEmpty() &&
					System.currentTimeMillis() < nextSaveTime &&
					i < nextSaveSector
				) {
					continue;
				}
			} else {
				final var output = new ByteArrayOutputStream();
				try (
					final var binput = MemoryInputStream.ofOffset(memory, position);
					final var cinput = compressor.inflater(binput)
				) {
					cinput.transferTo(output);
				} catch (Throwable t) {
					logger.trace("Swallowing, likely EOF", t);
				}

				if (output.size() != 0) {
					final byte[] bytes = output.toByteArray();

					mime = MagicUtils.test(bytes);
					results = find.scan(bytes);

					final String sample = new String(bytes, 0, Math.min(bytes.length, 64), StandardCharsets.UTF_8);
					safeSample = URLEncoder.encode(sample, StandardCharsets.UTF_8);

					logger.debug(
						"Captured {} bytes from sector {} ({}), type: {}, sample: {}",
						output.size(),
						i,
						position,
						mime,
						safeSample
					);
				} else {
					safeSample = null;
					mime = MagicUtils.test(memory, position);
					results = find.scan(memory, position, sectorSize);
					if (mime != null) {
						logger.debug("Found {} from sector {} ({})", mime, i, position);
					}
				}
			}

			// TODO: scour sector for rogue compressor headers
			//  Should we use an entropy encoder like zxcvbn to narrow down which sectors to scan?
			//  High-entropy sectors are far more likely to have successive compression frames.
			// TODO: Any entropy encoder should be bypassed with --paranoia
			// TODO: Any candidate headers (including deflate) should be included on --paranoia

			final long currentSector = i;
			final long lastDataSector = dataSector;
			final long lastEmptySector = emptySector;
			SqlUtils.transaction(connection, $ -> {
				metadataUpdate.setLong(1, currentSector);
				metadataUpdate.setLong(2, lastDataSector);
				metadataUpdate.setLong(3, lastEmptySector);
				metadataUpdate.execute();

				dataInsert.execute();

				if (!results.isEmpty()) {
					final var itr = Long2ObjectMaps.fastIterator(results);
					while (itr.hasNext()) {
						final var entry = itr.next();
						final var result = entry.getValue();

						final int id;

						stringInsert.setString(1, result.value());
						try (final var insert = stringInsert.executeQuery()) {
							if (insert.next()) {
								id = insert.getInt("id");
							} else {
								stringSelect.setString(1, result.value());
								try (final var select = stringSelect.executeQuery()) {
									id = select.getInt("id");
								}
							}
						}

						poiInsert.setLong(1, compressor.isCompressor() ? position : entry.getLongKey());
						poiInsert.setLong(2, compressor.isCompressor() ? entry.getLongKey() : 0L);
						poiInsert.setInt(3, id);
						poiInsert.addBatch();
					}
					poiInsert.execute();
				}

				if (mime == null) {
					return;
				}

				streamInsert.setLong(1, position);
				streamInsert.setString(2, CompressorRegistry.getCompressorMime(compressor));
				streamInsert.setString(3, Objects.requireNonNullElse(mime.getMimeType(), mime.getMessage()));
				streamInsert.execute();

				if (!compressor.isCompressor() || safeSample == null) {
					return;
				}

				foundInsert.setLong(1, position);
				foundInsert.setString(2, safeSample);
				foundInsert.execute();
			});
			nextSaveTime = System.currentTimeMillis() + (60 * 60 * 1000);
			nextSaveSector = currentSector + saveSectors;
		}

		final long lastDataSector = dataSector;
		final long lastEmptySector = emptySector;
		SqlUtils.transaction(connection, $ -> {
			metadataUpdate.setLong(1, sectors);
			metadataUpdate.setLong(2, lastDataSector);
			metadataUpdate.setLong(3, lastEmptySector);
			metadataUpdate.execute();

			if (lastDataSector > lastEmptySector) {
				dataInsert.setLong(1, lastEmptySector + 1L);
				dataInsert.setLong(2, sectors - lastEmptySector);
				dataInsert.addBatch();
			}
			dataInsert.execute();
		});
	}

	private static final VarHandle handle = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.nativeOrder());

	private static long bitOr(final byte[] bytes) {
		long or = 0L;
		final int length = bytes.length / Long.BYTES;
		for (int i = 0; i < length; i++) {
			or |= (long) handle.get(bytes, i * Long.BYTES);
		}
		for (int i = length * Long.BYTES; i < bytes.length; i++) {
			or |= bytes[i];
		}
		return or;
	}
}
