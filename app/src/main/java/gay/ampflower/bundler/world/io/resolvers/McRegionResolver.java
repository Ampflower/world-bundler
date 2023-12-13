package gay.ampflower.bundler.world.io.resolvers;

import gay.ampflower.bundler.utils.function.FileResolver;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class McRegionResolver implements FileResolver {
	private final String prefix;
	private final String extension;
	private final Pattern pattern;

	public McRegionResolver(String prefix, String extension, Pattern pattern) {
		this.prefix = prefix;
		this.extension = extension;
		this.pattern = pattern;
	}

	@Override
	public String fileName(final int x, final int y) {
		return prefix + '.' + x + '.' + y + extension;
	}

	@Override
	public boolean match(final String name) {
		return pattern.matcher(name).matches();
	}

	@Override
	public LongSet iterate(Path path) throws IOException {
		final var matcher = pattern.matcher("");
		final var positions = new LongOpenHashSet();

		try (final var paths = Files.newDirectoryStream(path)) {
			for (final Path p : paths) {
				final var name = p.getFileName().toString();
				matcher.reset(name);

				if (!matcher.matches()) {
					continue;
				}

				final int x = Integer.parseInt(name, matcher.start(1), matcher.end(1), 10);
				final int y = Integer.parseInt(name, matcher.start(2), matcher.end(2), 10);

				positions.add(((long) x << 32) | ((long) y & 0xFFFFFFFFL));
			}
		}

		return positions;
	}
}
