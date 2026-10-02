package gay.ampflower.bundler.utils;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.entries.MagicEntries;
import gay.ampflower.bundler.compress.GZipCompressor;
import gay.ampflower.bundler.utils.io.IoUtils;
import org.jetbrains.annotations.CheckReturnValue;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.lang.foreign.MemorySegment;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class MagicUtils {
	private static final Logger logger = LogUtils.logger();
	private static final MagicEntries entries = new MagicEntries();

	/**
	 * Chosen by a fair fish roll.
	 *
	 * @see <a href="https://xkcd.com/221">xkcd: Random Number</a>
	 */
	private static final int magicHeader = 7359;

	static {
		for (final Path path : PlatformUtils.findConfigs(
			false,
			true,
			true,
			true,
			false,
			// Binary file, cannot be used.
			// "file/misc/magic.mgc",
			// "magic.mgc"
			"magic"
		)) {
			logger.debug("Found {}", path);

			try (final BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				entries.readEntries(
					reader,
					(line, details, exception) -> logger.debug("Error parsing {}: {}\n{}", path, line, details, exception)
				);
			} catch (IOException e) {
				logger.debug("Could not read {}", path, e);
			}
		}

		final var magicStream = MagicEntries.class.getResourceAsStream("/magic.gz");
		if (magicStream != null) {
			try (
				magicStream;
				final var magicInflater = GZipCompressor.INSTANCE.inflater(magicStream);
				final var inflaterReader = new InputStreamReader(magicInflater);
				final var readerBuffer = new BufferedReader(inflaterReader)
			) {
				entries.readEntries(
					readerBuffer,
					(line, details, exception) -> logger.warn("Error parsing built-in: {}\n{}", line, details, exception)
				);
			} catch (IOException e) {
				logger.warn("Could not read built-in", e);
			}
		}

		entries.optimizeFirstBytes();
	}

	@CheckReturnValue
	public static Optional<ContentInfo> test(final InputStream stream) throws IOException {
		final byte[] buf = IoUtils.markRead(stream, magicHeader);
		if (buf == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(test(buf));
	}

	@CheckReturnValue
	public static ContentInfo test(final PushbackInputStream stream) throws IOException {
		return test(IoUtils.markRead(stream, magicHeader));
	}

	@CheckReturnValue
	public static ContentInfo test(final SeekableByteChannel channel, final long position) throws IOException {
		return test(IoUtils.markRead(channel, position, magicHeader));
	}

	@CheckReturnValue
	public static ContentInfo test(final FileChannel channel, final long position) throws IOException {
		return test(IoUtils.markRead(channel, position, magicHeader));
	}

	@CheckReturnValue
	public static ContentInfo test(final MemorySegment segment, final long position) {
		return test(IoUtils.markRead(segment, position, magicHeader));
	}

	@CheckReturnValue
	public static ContentInfo test(final byte[] file) {
		return entries.findMatch(file);
	}
}
