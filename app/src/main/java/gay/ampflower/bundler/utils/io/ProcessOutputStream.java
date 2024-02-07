package gay.ampflower.bundler.utils.io;

import gay.ampflower.bundler.utils.LogUtils;
import org.slf4j.Logger;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class ProcessOutputStream extends FilterOutputStream {
	private static final Logger logger = LogUtils.logger();

	private final Process process;
	private final OutputStream processOutput;
	private final Thread ioWorker;

	private volatile boolean processOutputClosed;
	private volatile IOException fault;

	public ProcessOutputStream(final Process process, final OutputStream stream) {
		super(process.getOutputStream());
		this.process = process;
		this.processOutput = stream;

		this.ioWorker = IoUtils.asyncPipe(process.getInputStream(), stream, ioe -> this.fault = ioe, this::closeProcess);
	}

	private void checkOpen() throws IOException {
		if (processOutputClosed) {
			throw new IOException("closed");
		}
		if (fault != null) {
			throw new IOException("faulted", fault);
		}
	}

	@Override
	public void write(final byte[] b) throws IOException {
		checkOpen();
		out.write(b);
	}

	@Override
	public void write(final byte[] b, final int off, final int len) throws IOException {
		checkOpen();
		out.write(b, off, len);
	}

	@Override
	public void write(final int b) throws IOException {
		checkOpen();
		out.write(b);
	}

	@Override
	public void flush() throws IOException {
		checkOpen();
		out.flush();
	}

	@Override
	public void close() throws IOException {
		if (processOutputClosed) {
			return;
		}
		try (processOutput) {
			super.close();
			try {
				// Stalls to allow the thread to flush.
				// It's fairly reasonable to expect this if the target is a ByteArrayOutputStream
				ioWorker.join();
			} catch (InterruptedException e) {
				throw new AssertionError(e);
			}
			closeProcess();
			processOutput.flush();
			if (fault != null) {
				throw new IOException("Exception while closing", fault);
			}
		}
	}

	private void closeProcess() {
		logger.trace("{} closed", process.pid());
		processOutputClosed = true;

		try {
			final int exit = process.waitFor();
			if (exit != 0) {
				fault = new IOException("Process closed abnormally: " + exit);
			}
		} catch (InterruptedException interruptedException) {
			throw new AssertionError(interruptedException);
		}
	}
}
