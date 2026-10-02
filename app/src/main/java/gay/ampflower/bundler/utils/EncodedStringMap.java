package gay.ampflower.bundler.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CoderMalfunctionError;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;

/**
 * @author Ampflower
 * @since ${version}
 **/
@NotNullByDefault
public final class EncodedStringMap {
	private static final Logger logger = LogUtils.logger();

	private static final VarHandle arrayHandle = MethodHandles.byteArrayViewVarHandle(
		long[].class,
		ByteOrder.nativeOrder()
	);
	private static final VarHandle memoryHandle = ValueLayout.JAVA_LONG_UNALIGNED.withOrder(ByteOrder.nativeOrder())
		.varHandle();

	private static final VarHandle byteHandle = ValueLayout.JAVA_BYTE.varHandle();

	public static final EncodedStringMap NONE = new EncodedStringMap();

	private static final long[] masks = {
		0xFFFFFFFF_FFFFFFFFL,
		0xFFFFFFFF_FFFFFF00L,
		0xFFFFFFFF_FFFF0000L,
		0xFFFFFFFF_FF000000L,
		0xFFFFFFFF_00000000L,
		0xFFFFFF00_00000000L,
		0xFFFF0000_00000000L,
		0xFF000000_00000000L,
	};

	static {
		if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
			for (int i = 0; i < masks.length; i++) {
				masks[i] = Long.reverseBytes(masks[i]);
			}
		}
	}

	private static final byte[] empty = new byte[8];
	private final Long2ObjectMap<Bucket> backing;
	private final int minLength;

	private EncodedStringMap() {
		this.backing = Long2ObjectMaps.emptyMap();
		this.minLength = Integer.MAX_VALUE;
	}

	public EncodedStringMap(
		final Collection<Charset> charsets,
		final Collection<String> unprocessedStrings
	) {
		final List<Charset> encoders = new ArrayList<>();
		final Set<String> strings = new HashSet<>();
		int max = 0, min = Integer.MAX_VALUE;

		for (final var charset : charsets) {
			if (!charset.canEncode()) {
				logger.warn("{} cannot encode text", charset);
				continue;
			}
			encoders.add(charset);
		}

		for (final var value : unprocessedStrings) {
			if (value.isEmpty()) {
				continue;
			}
			max = Math.max(max, value.length());
			strings.add(value.intern());
		}

		final var work = new Long2ObjectOpenHashMap<Int2ObjectMap<List<Result0>>>();

		final var bytes = new byte[Math.multiplyExact(max, 8)];
		final var bytebuf = ByteBuffer.wrap(bytes);
		bytebuf.order(ByteOrder.nativeOrder());

		int encoded = 0;

		for (final var charset : encoders) {
			final var encoder = charset.newEncoder();
			for (final var value : strings) {
				try {
					bytebuf.clear();
					final var result = encoder.encode(CharBuffer.wrap(value), bytebuf, true);

					if (result.isError()) {
						logger.warn("{} could not encode {}, skipping.\n{}", charset, value, result);
						continue;
					}

					final int length = bytebuf.position();

					if (length == 0) {
						logger.warn("{} could not encode {}, skipping. Is this a bug? {}", charset, value, bytebuf);
					}

					final byte[] array = new byte[length];
					bytebuf.get(0, array);
					min = Math.min(min, length);

					if (length < 8) {
						bytebuf.put(empty, 0, 8 - length);
					}

					final long index = bytebuf.getLong(0);

					work.computeIfAbsent(index, $ -> new Int2ObjectOpenHashMap<>())
						.computeIfAbsent(length, $ -> new ArrayList<>())
						// TODO: known fast stable hash algorithm
						.add(new Result0(value, charset, array, Arrays.hashCode(array)));

					encoded++;
				} catch (CoderMalfunctionError e) {
					logger.warn("{} could not encode {}, skipping.", charset, value, e);
				} finally {
					encoder.reset();
				}
			}
		}

		if (encoded == 0) {
			logger.debug("Could not encode anything. Returning.");
			this.backing = Long2ObjectMaps.emptyMap();
			this.minLength = Integer.MAX_VALUE;
			return;
		}

		logger.debug("Encoded {} strings with {} charsets {} times.", strings.size(), encoders.size(), encoded);

		final var backing = new Long2ObjectOpenHashMap<Bucket>();

		final var itr = Long2ObjectMaps.fastIterator(work);
		while (itr.hasNext()) {
			final var entry = itr.next();
			backing.put(entry.getLongKey(), Bucket.of(entry.getValue()));
		}

		this.backing = Long2ObjectMaps.unmodifiable(backing);
		this.minLength = min;
	}

	public boolean canSearch(final long length) {
		return this.minLength < length && !this.backing.isEmpty();
	}

	@CheckReturnValue
	public @Nullable Result lookup(final byte[] bytes) {
		return this.lookup(bytes, 0, bytes.length);
	}

	@CheckReturnValue
	public @Nullable Result lookup(final byte[] bytes, final int offset, int length) {
		if (length < this.minLength) {
			return null;
		}

		if (offset + length > bytes.length) {
			length = bytes.length - offset;
		}

		return this.lookup(
			ArrayUtils.readLong(bytes, offset, arrayHandle),
			length,
			len -> {
				int hash = 0;
				len += offset;
				for (int i = offset; i < len; i++) {
					hash = hash * 31 + bytes[i];
				}
				return hash;
			},
			result -> Arrays.equals(result.encoded, 0, result.length(), bytes, offset, offset + result.length())
		);
	}

	@CheckReturnValue
	public @Nullable Result lookup(final MemorySegment memory, final long position, int length) {
		if (length < this.minLength) {
			return null;
		}

		if (position + length > memory.byteSize()) {
			length = Mint.clampAsInt(memory.byteSize() - position);
		}

		return lookup(
			ArrayUtils.readLong(memory, position, arrayHandle, memoryHandle),
			length,
			len -> {
				int hash = 0;
				long ll = len + position;
				for (long j = position; j < ll; j++) {
					hash = hash * 31 + (byte) byteHandle.get(memory, j);
				}
				return hash;
			},
			result -> {
				final var encoded = MemorySegment.ofArray(result.encoded());
				final var spliced = memory.asSlice(position, encoded.byteSize());
				return encoded.mismatch(spliced) < 0;
			}
		);
	}

	@CheckReturnValue
	private @Nullable Result lookup(
		final long sample,
		final int length,
		final IntUnaryOperator lengthToHash,
		final Predicate<Result0> predicate
	) {
		for (int i = 0; i < 8; i++) {
			final long index = sample & masks[i];

			final var bucket = this.backing.get(index);

			if (bucket == null) {
				continue;
			}

			final var lengths = bucket.lengths();
			for (int j = 0; j < lengths.length && lengths[j] < length; j++) {
				final int hash = lengthToHash.applyAsInt(j);
				final var results = bucket.results().get(hash);

				if (results == null || results.isEmpty()) {
					continue;
				}

				for (final var result : results) {
					if (predicate.test(result)) {
						return result;
					}
				}
			}
		}

		return null;
	}

	@CheckReturnValue
	private Long2ObjectMap<Result> scan(
		final long total,
		final long offset,
		long span,
		long limit,
		final ScanDriver<Result> function
	) {
		if (offset + span > total) {
			span = total - offset - this.minLength;
		}

		if (offset + limit > total) {
			limit = total - offset;
		}

		if (!this.canSearch(limit)) {
			return Long2ObjectMaps.emptyMap();
		}

		final var results = new Long2ObjectOpenHashMap<Result>();

		for (int i = 0; i < span; i++) {
			final Result result = function.apply(offset + i, Mint.clampAsInt(limit - i));

			if (result != null) {
				results.put(offset + i, result);
			}
		}

		return Long2ObjectMaps.unmodifiable(results);
	}

	@CheckReturnValue
	public Long2ObjectMap<Result> scan(final byte[] bytes) {
		return scan(bytes, 0, bytes.length, bytes.length);
	}

	@CheckReturnValue
	public Long2ObjectMap<Result> scan(final byte[] bytes, final int offset, final int span, final int limit) {
		return scan(bytes.length, offset, span, limit, (position, length) -> lookup(bytes, (int) position, length));
	}

	@CheckReturnValue
	public Long2ObjectMap<Result> scan(final MemorySegment memory) {
		return scan(memory, 0, memory.byteSize(), memory.byteSize());
	}

	@CheckReturnValue
	public Long2ObjectMap<Result> scan(final MemorySegment memory, final long offset, final long length) {
		return scan(memory, offset, length, memory.byteSize() - offset);
	}

	@CheckReturnValue
	public Long2ObjectMap<Result> scan(
		final MemorySegment memory,
		final long offset,
		final long span,
		final long limit
	) {
		return scan(memory.byteSize(), offset, span, limit, (position, length) -> lookup(memory, position, length));
	}

	@FunctionalInterface
	private interface ScanDriver<O> {
		O apply(final long position, final int length);
	}

	public sealed interface Result {
		String value();

		Charset charset();

		int length();
	}

	private record Bucket(
		int[] lengths,
		Int2ObjectMap<List<Result0>> results
	) {
		private static Bucket of(Int2ObjectMap<List<Result0>> results) {
			final var array = new int[results.size()];
			final var hashToResults = new Int2ObjectOpenHashMap<List<Result0>>();

			{
				final var itr = Int2ObjectMaps.fastIterator(results);
				int index = 0;
				while (itr.hasNext()) {
					final var entry = itr.next();
					array[index++] = entry.getIntKey();
					final var list = entry.getValue();
					for (final var result : list) {
						hashToResults.computeIfAbsent(result.hash(), $ -> new ArrayList<>()).add(result);
					}
				}
			}

			{
				final var itr = Int2ObjectMaps.fastIterator(hashToResults);
				while (itr.hasNext()) {
					final var entry = itr.next();
					entry.setValue(List.copyOf(entry.getValue()));
				}
			}

			Arrays.sort(array);

			return new Bucket(array, hashToResults);
		}

		public int min() {
			return this.lengths[0];
		}

		public int max() {
			return this.lengths[this.lengths.length - 1];
		}
	}

	private record Result0(
		String value,
		Charset charset,
		byte[] encoded,
		int hash
	) implements Result {
		@Override
		public int length() {
			return encoded.length;
		}
	}
}
