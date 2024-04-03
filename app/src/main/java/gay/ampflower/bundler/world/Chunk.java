package gay.ampflower.bundler.world;

import gay.ampflower.bundler.nbt.Nbt;
import gay.ampflower.bundler.nbt.io.NbtWriter;
import gay.ampflower.bundler.utils.ArrayUtils;
import gay.ampflower.bundler.utils.io.IoUtils;

import java.beans.Transient;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;

/**
 * @author Ampflower
 * @since ${version}
 **/
public record Chunk(
	int x,
	int y,
	int timestamp,
	Nbt<?> nbt,
	@Transient
	int size
) {

	public Chunk(int x, int y, int timestamp, byte[] array) {
		this(x, y, timestamp, array.length == 0 ? null : IoUtils.verifyNbt(array, Region.getChunkIndex(x, y)), array.length);
	}

	public Chunk(int rx, int ry, int i, int timestamp, byte[] array) {
		this(Region.getChunkX(rx, i), Region.getChunkY(ry, i), timestamp, array);
	}

	public Chunk pos(int x, int y) {
		return new Chunk(x, y, timestamp, nbt, size);
	}

	@Deprecated
	public byte[] array() throws IOException {
		if (isEmpty()) {
			return ArrayUtils.SENTINEL_BYTES;
		}

		final var output = new ByteArrayOutputStream(size);
		try (final var writer = new NbtWriter(output)) {
			writer.push(nbt);
		}
		return output.toByteArray();
	}

	public boolean isEmpty() {
		return nbt == null;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final Chunk chunk = (Chunk) o;
		return x == chunk.x
			&& y == chunk.y
			&& timestamp == chunk.timestamp
			&& Objects.equals(nbt, chunk.nbt);
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y, timestamp, nbt);
	}

	@Override
	public String toString() {
		return x + ", " + y + " @ " + timestamp + " (" + size + "): " + this.nbt;
	}
}
