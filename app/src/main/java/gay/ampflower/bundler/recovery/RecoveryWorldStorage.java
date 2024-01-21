package gay.ampflower.bundler.recovery;

import gay.ampflower.bundler.world.io.ChunkStorage;
import gay.ampflower.bundler.world.io.resolvers.FileResolvers;

import java.io.IOException;
import java.nio.file.Path;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class RecoveryWorldStorage {
	private final Path worldRoot;
	public final ChunkStorage entities, poi, regions;
	public final DataStorage data;

	public RecoveryWorldStorage(Path worldRoot, FileResolvers fileResolver) {
		this.worldRoot = worldRoot;
		entities = fileResolver.createChunkStorage(worldRoot.resolve("entities"));
		poi = fileResolver.createChunkStorage(worldRoot.resolve("poi"));
		regions = fileResolver.createChunkStorage(worldRoot.resolve("regions"));
		data = new DataStorage(worldRoot.resolve("data"));
	}

	public RecoveryWorldStorage init() throws IOException {
		entities.init();
		poi.init();
		regions.init();

		return this;
	}
}
