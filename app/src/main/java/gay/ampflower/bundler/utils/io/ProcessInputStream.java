package gay.ampflower.bundler.utils.io;

import gay.ampflower.bundler.utils.LogUtils;
import org.slf4j.Logger;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class ProcessInputStream extends FilterInputStream {
	private static final Logger logger = LogUtils.logger();

	private final Process process;
	private final InputStream processInput;
	private volatile IOException fault;

	public ProcessInputStream(final Process process, final InputStream stream) {
		super(process.getInputStream());
		this.process = process;
		this.processInput = stream;

		IoUtils.asyncPipe(stream, process.getOutputStream(), this::intrude, this::closeProcess);
	}

	private void checkOpen() throws IOException {
		if (fault != null) {
			throw new IOException(process.pid() + " faulted", fault);
		}
	}

	@Override
	public int read() throws IOException {
		checkOpen();
		return in.read();
	}

	@Override
	public int read(final byte[] b) throws IOException {
		checkOpen();
		return in.read(b);
	}

	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		checkOpen();
		return in.read(b, off, len);
	}

	@Override
	public void close() throws IOException {
		super.close();
		closeProcess();
		if (fault != null) {
			throw new IOException("Exception while closing", fault);
		}
	}

	private void closeProcess() {
		logger.trace("{} closed", process.pid());

		try {
			final int exit = process.waitFor();
			if (exit != 0) {
				intrude(new IOException("Process closed abnormally: " + exit));
			}
		} catch (InterruptedException interruptedException) {
			throw new AssertionError(interruptedException);
		}
	}

	private void intrude(IOException fault) {
		final var old = this.fault;
		if (old != null) {
			fault.addSuppressed(old);
		}
		this.fault = fault;
	}
}
