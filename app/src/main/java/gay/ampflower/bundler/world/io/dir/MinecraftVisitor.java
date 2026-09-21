package gay.ampflower.bundler.world.io.dir;

import gay.ampflower.bundler.utils.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class MinecraftVisitor extends AbstractVisitor<Path, RegionData> {
	private static final Logger logger = LogUtils.logger();

	private FileSystem fileSystem;
	private Path levelData;
	private Path region;
	private final List<RegionData> worldRoots = new ArrayList<>();
	private final List<RegionData> dimRoots = new ArrayList<>();

	protected MinecraftVisitor() {
		super(RegionData::new, Resolver.Paths.inst);
	}

	@Override
	public DirectoryMeta<RegionData> meta() {
		return this.meta(Map.of(
			"worlds", List.copyOf(worldRoots),
			"dimensions", List.copyOf(dimRoots)
		));
	}

	@Override
	protected FileVisitResult onDir(
		final RegionData parent,
		final Path dir,
		final BasicFileAttributes attrs
	) throws IOException {
		final var fs = dir.getFileSystem();
		if (fs != this.fileSystem) {
			this.fileSystem = fs;
			this.levelData = fs.getPath("level.dat");
			this.region = fs.getPath("region");
		}

		if (dir.endsWith(region)) {
			logger.debug("Found dimension {}", parent);
			this.dimRoots.add(parent);
		}

		return FileVisitResult.CONTINUE;
	}

	@Override
	protected FileVisitResult onFile(
		final RegionData parent,
		final Path path,
		final BasicFileAttributes attrs
	) throws IOException {
		if (path.endsWith(levelData)) {
			logger.debug("Found root {}", parent);
			this.worldRoots.add(parent);
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	protected void reset() {
		super.reset();
		levelData = null;
	}
}
