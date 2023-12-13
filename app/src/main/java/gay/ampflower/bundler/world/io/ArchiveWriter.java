package gay.ampflower.bundler.world.io;

import gay.ampflower.bundler.utils.LevelCompressor;
import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.utils.SizeUtils;
import gay.ampflower.bundler.utils.io.CountingOutputStream;
import gay.ampflower.bundler.utils.io.ResettableByteArrayOutputStream;
import gay.ampflower.bundler.utils.pos.Pos2i;
import gay.ampflower.bundler.world.io.dir.DirectoryReader;
import gay.ampflower.bundler.world.region.BundlerHandler;
import org.slf4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class ArchiveWriter implements WorldWriter {
	private static final Logger logger = LogUtils.logger();

	private final OutputStream stream;

	ArchiveWriter(OutputStream stream) {
		this.stream = stream;
	}

	public void run(Path regionIn) throws IOException {
		final var meta = DirectoryReader.run(regionIn);
		final var list = meta.list;

		try (final var fos = new FileOutputStream(FileDescriptor.out);
			  final var cos = new CountingOutputStream(fos);
			  final var zos = new ZipOutputStream(cos, StandardCharsets.UTF_8);
			  final var ros = new ResettableByteArrayOutputStream((int) SizeUtils.MiB * 8, 65536)) {
			zos.setMethod(ZipOutputStream.STORED);

			final var cksum = new CRC32();

			final var itr = list.listIterator(list.size());
			while (itr.hasPrevious()) {
				final var dir = itr.previous();
				{
					final var rel = regionIn.relativize(dir.dir);
					if (!rel.toString().isEmpty()) {
						final var entry = new ZipEntry(rel + "/");
						if (!entry.isDirectory()) throw new AssertionError(rel.toString());
						entry.setMethod(ZipEntry.STORED);
						entry.setSize(0);
						entry.setCrc(0);
						zos.putNextEntry(entry);
					}
				}

				// TODO: move to internal func
				for (final var file : dir.paths) {
					final var rel = regionIn.relativize(file);
					final var entry = new ZipEntry(rel.toString());

					try (final var fin = Files.newInputStream(file);
						  final var pin = new PushbackInputStream(fin, 8)) {
						final var compressor = LevelCompressor.getFileCompressor(pin);
						final byte[] bytes;

						try (final var inf = compressor.inflater(pin)) {
							bytes = inf.readAllBytes();
						}

						cksum.update(bytes);
						entry.setCrc(cksum.getValue());
						cksum.reset();

						entry.setSize(bytes.length);
						if (compressor != LevelCompressor.NONE) {
							entry.setComment(compressor.name());
						}

						zos.putNextEntry(entry);
						zos.write(bytes);
					}
				}

				final var bundle = BundlerHandler.INSTANCE;

				for (final var e : dir.regions.entrySet()) {
					final var fr = e.getKey();
					final var regions = e.getValue();
					final var storage = fr.createChunkStorage(dir.dir);

					final var regionItr = regions.iterator();
					while (regionItr.hasNext()) {
						final var regionPos = regionItr.nextLong();
						final int x = Pos2i.x(regionPos);
						final int y = Pos2i.y(regionPos);

						final var region = storage.readRegion(x, y);

						if (region == null) {
							logger.error("Failed to read {}, {} from {} at {} for {}", x, y, storage, dir.dir, fr);
							continue;
						}

						final var entry = new ZipEntry(regionIn.relativize(dir.dir) + fr.regionRes.fileName(x, y) + ".bundle");

						bundle.writeRegion(ros, region);

						entry.setCrc(ros.checksum(cksum));
						cksum.reset();

						entry.setSize(ros.getCount());

						zos.putNextEntry(entry);
						ros.transferTo(zos);

						ros.close();
					}
				}
			}

			zos.finish();
			logger.info("Written {} bytes", SizeUtils.displaySize(cos.getTransferred()));
		}
	}

	@Override
	public void close() throws Exception {
		stream.close();
	}
}
