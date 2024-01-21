package gay.ampflower.bundler.world;

import gay.ampflower.bundler.nbt.Nbt;

/**
 * @author Ampflower
 * @since ${version}
 **/
public record PotentialChunk(byte[] bytes, Nbt<?> parsed) {
}
