package gay.ampflower.bundler.world.io.dir;

import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.utils.SizeUtils;
import gay.ampflower.bundler.world.io.resolvers.FileResolvers;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;

public final class DirectoryData {
	private static final Logger logger = LogUtils.logger();

	public final Path dir;
	public final Set<DirectoryData> dirs = new HashSet<>();
	public final Set<Path> paths = new HashSet<>();
	public final LongSet mcc = new LongOpenHashSet();
	public final LongSet alpha = new LongOpenHashSet();
	public final EnumMap<FileResolvers, LongSet> regions = new EnumMap<>(FileResolvers.class);
	public long size;

	DirectoryData(Path dir) {
		this.dir = dir;
		if (!Files.isDirectory(dir)) {
			throw new AssertionError(dir + " is not a directory");
		}
	}

	public void push(Path path, BasicFileAttributes attr) {
		this.size += attr.size();

		if (FileResolvers.Alpha.matchesChunk(path)) {
			throw new UnsupportedOperationException("Alpha worlds aren't supported at this time. Chunk: " + path);
		}

		var pos = FileResolvers.Anvil.getChunkCoordinate(path);

		if (pos != null) {
			logger.trace("{} -> {} ({})", path, pos, pos.toLong());
			this.mcc.add(pos.toLong());
			return;
		}

		for (var resolver : FileResolvers.resolvers) {
			pos = resolver.getRegionCoordinate(path);
			if (pos != null) {
				this.regions.computeIfAbsent(resolver, $ -> new LongOpenHashSet()).add(pos.toLong());
				return;
			}
		}

		paths.add(path);
	}

	public void push(DirectoryData dir) {
		this.dirs.add(dir);
	}

	@Override
	public int hashCode() {
		return this.dir.hashCode();
	}

	@Override
	public String toString() {
		return "DirectoryData(" + dir + "){" +
			"\n\tpaths=" + paths +
			",\n\tmcc=" + mcc +
			",\n\talpha=" + alpha +
			",\n\tregions=" + regions +
			",\n\tsize=" + SizeUtils.displaySize(size) +
			"\n}";
	}
}
