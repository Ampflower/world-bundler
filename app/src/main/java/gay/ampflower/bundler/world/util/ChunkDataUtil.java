package gay.ampflower.bundler.world.util;

import gay.ampflower.bundler.nbt.Nbt;
import gay.ampflower.bundler.nbt.NbtCompound;
import gay.ampflower.bundler.utils.pos.Pos2i;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class ChunkDataUtil {

	public static Pos2i getPosition(Nbt<?> parsed) {
		if (parsed instanceof NbtCompound compound) {
			boolean hasX = compound.hasKey("xPos");
			boolean hasZ = compound.hasKey("zPos");

			if (hasX && hasZ) {
				return new Pos2i(
					compound.getInt("xPos"),
					compound.getInt("zPos")
				);
			}
		}
		return null;
	}

	public static long getLastUpdate(Nbt<?> parsed) {
		if (parsed instanceof NbtCompound compound) {
			boolean hasLastUpdate = compound.hasKey("LastUpdate");

			if (hasLastUpdate) {
				return compound.getLong("LastUpdate");
			}
		}
		return Long.MIN_VALUE;
	}
}
