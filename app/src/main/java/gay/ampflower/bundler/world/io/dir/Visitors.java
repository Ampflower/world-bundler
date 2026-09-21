package gay.ampflower.bundler.world.io.dir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author Ampflower
 * @since ${version}
 **/
public
final class Visitors {

	public static DirectoryMeta<PathData<Path>> walkTree(final Path start) throws IOException {
		try (final var visitor = new AbstractVisitor<>(PathData::new, Resolver.Paths.inst)) {
			Files.walkFileTree(start, visitor);
			return visitor.meta();
		}
	}

	public static DirectoryMeta<RegionData> walkMinecraft(final Path start) throws IOException {
		try (final var visitor = new MinecraftVisitor()) {
			Files.walkFileTree(start, visitor);
			return visitor.meta();
		}
	}
}
