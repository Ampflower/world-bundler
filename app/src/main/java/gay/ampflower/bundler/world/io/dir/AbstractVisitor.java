package gay.ampflower.bundler.world.io.dir;

import gay.ampflower.bundler.utils.LogUtils;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Ampflower
 * @since ${version}
 */
public class AbstractVisitor<T, D extends PathData<T>> implements FileVisitor<T>, AutoCloseable {
	private static final Logger logger = LogUtils.logger();

	private long size;
	private long dirs;
	private long files;
	private long error;

	private final List<D> visited = new ArrayList<>();
	private final Deque<D> stack = new ArrayDeque<>();

	private final Function<T, D> dataSupplier;
	private final Resolver<T> resolver;

	protected AbstractVisitor(final Function<T, D> dataSupplier, final Resolver<T> resolver) {
		this.dataSupplier = dataSupplier;
		this.resolver = resolver;
	}

	public DirectoryMeta<D> meta() {
		return meta(Map.of());
	}

	protected DirectoryMeta<D> meta(final Map<String, List<D>> misc) {
		return new DirectoryMeta<>(List.copyOf(visited), misc, size, dirs, files, error);
	}

	@Override
	@MustBeInvokedByOverriders
	public FileVisitResult preVisitDirectory(final T dir, final BasicFileAttributes attrs) throws IOException {
		logger.debug("Visiting dir {} with attributes {}", dir, attrs);
		final var parent = this.top();
		if (parent != null && !parent.dir.equals(this.resolver.getParent(dir))) {
			logger.error("{}", this.stack);
			throw new AssertionError(parent + " isn't parent of " + dir + "; missing pop???");
		}
		final var data = this.dataSupplier.apply(dir);

		this.stack.push(data);

		if (parent != null) {
			parent.push(this.resolver.getFileName(dir), data);
		}

		this.dirs++;
		return onDir(parent, dir, attrs);
	}

	@Override
	@MustBeInvokedByOverriders
	public FileVisitResult visitFile(final T file, final BasicFileAttributes attrs) throws IOException {
		final var parent = this.top();
		if (parent == null) {
			throw new IllegalStateException("in: " + root() + ", at: " + file);
		}
		this.files++;
		this.size += attrs.size();
		parent.push(this.resolver.getFileName(file), file, attrs);
		return onFile(parent, file, attrs);
	}

	@Override
	@MustBeInvokedByOverriders
	@CheckReturnValue
	public FileVisitResult visitFileFailed(final T file, final IOException exc) throws IOException {
		logger.warn("Failed to visit {}", file, exc);
		this.error++;
		return FileVisitResult.CONTINUE;
	}

	@Override
	@MustBeInvokedByOverriders
	public FileVisitResult postVisitDirectory(final T dir, final IOException exc) throws IOException {
		if (exc != null) {
			logger.warn("Walking {} finished with error.", dir, exc);
		}
		final var last = stack.poll();
		if (last == null) {
			throw new AssertionError("Popped null data for " + dir);
		}
		if (!last.dir.equals(dir)) {
			throw new AssertionError(last + " popped for " + dir);
		}
		visited.add(last);
		return FileVisitResult.CONTINUE;
	}

	protected final T root() {
		final var root = this.stack.peekLast();
		if (root == null) {
			return null;
		}
		return root.dir;
	}

	protected final D top() {
		return this.stack.peek();
	}

	protected FileVisitResult onDir(final D parent, final T dir, final BasicFileAttributes attrs) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	protected FileVisitResult onFile(final D parent, final T path, final BasicFileAttributes attrs) throws IOException {
		return FileVisitResult.CONTINUE;
	}

	@MustBeInvokedByOverriders
	protected void reset() {
		stack.clear();
		visited.clear();
		size = 0L;
		dirs = 0L;
		files = 0L;
		error = 0L;
	}

	@Override
	public void close() {
		if (!this.stack.isEmpty()) {
			throw new AssertionError("Data present in lifo: " + this.stack);
		}

		logger.info("Walking complete, {}", this.meta());
		for (var dir : this.visited) {
			logger.info("{} -> {}", dir.paths, dir);
		}

		this.reset();
	}
}
