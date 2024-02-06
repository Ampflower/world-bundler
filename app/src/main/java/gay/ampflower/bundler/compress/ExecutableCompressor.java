package gay.ampflower.bundler.compress;

import gay.ampflower.bundler.utils.ArrayUtils;
import gay.ampflower.bundler.utils.IoUtils;
import gay.ampflower.bundler.utils.LogUtils;
import gay.ampflower.bundler.utils.io.ProcessOutputStream;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Ampflower
 * @since ${version}
 **/
public record ExecutableCompressor(List<String> deflater, List<String> inflater, byte[] magic) implements Compressor {
	private static final Logger logger = LogUtils.logger();

	public ExecutableCompressor(String deflater, String inflater, byte[] magic) {
		this(tokenize(deflater), tokenize(inflater), magic);
	}

	public ExecutableCompressor(String cat) {
		this(List.of(cat), List.of(cat), ArrayUtils.SENTINEL_BYTES);
	}

	private static List<String> tokenize(final String exec) {
		final String[] tokens;
		final var tokenizer = new StringTokenizer(exec);
		tokens = new String[tokenizer.countTokens()];

		for (int i = 0; tokenizer.hasMoreElements(); i++) {
			tokens[i] = tokenizer.nextToken();
		}

		return List.of(tokens);
	}

	private static Process start(List<String> args) throws IOException {
		final Process process = new ProcessBuilder(args)
			// We don't really want any of the error stream.
			.redirectError(ProcessBuilder.Redirect.INHERIT)
			.start();
		logger.debug("Started {} @ {}", args, process.pid());
		return process;
	}

	@Override
	public OutputStream deflater(final OutputStream stream) throws IOException {
		final Process process = start(deflater);

		process.onExit()
			.thenAccept(proc -> logger.debug("Deflater {} @ {} exited with {}", deflater.get(0), proc.pid(), proc.exitValue()));

		return new ProcessOutputStream(process, stream);
	}

	@Override
	public InputStream inflater(final InputStream stream) throws IOException {
		final Process process = start(inflater);

		process.onExit()
			.thenAccept(proc -> logger.debug("Inflater {} @ {} exited with {}", deflater.get(0), proc.pid(), proc.exitValue()));

		IoUtils.asyncPipe(stream, process.getOutputStream(), t -> logger.warn("", t), () -> logger.trace("Finished {}", process.pid()));

		return process.getInputStream();
	}

	@Override
	public boolean compatible(final PushbackInputStream stream) throws IOException {
		return false;
	}

	@Override
	public boolean compatible(final byte[] array) {
		return ArrayUtils.startsWith(array, magic);
	}
}
