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

public final class RegionData extends PathData<Path> {
	private static final Logger logger = LogUtils.logger();

	public final LongSet mcc = new LongOpenHashSet();
	public final LongSet alpha = new LongOpenHashSet();
	public final EnumMap<FileResolvers, LongSet> regions = new EnumMap<>(FileResolvers.class);

	public RegionData(final Path dir) {
		super(dir);
		if (!Files.isDirectory(dir)) {
			throw new AssertionError(dir + " is not a directory");
		}
	}

	@Override
	public void push(final String name, final Path path, final BasicFileAttributes attr) {
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

		super.push(name, path);
	}

	@Override
	public String toString() {
		return "RegionData(" + dir + "){" +
				 "\n\tpaths=" + paths +
				 ",\n\tmcc=" + mcc +
				 ",\n\talpha=" + alpha +
				 ",\n\tregions=" + regions +
				 ",\n\tsize=" + SizeUtils.displaySize(size) +
				 "\n}";
	}
}
