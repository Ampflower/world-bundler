package gay.ampflower.bundler.world.io.dir;

import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class PathData<T> {
	public final T dir;

	public final Set<T> paths = new HashSet<>();
	public final Map<String, T> pathMap = new HashMap<>();
	public final Set<PathData<T>> dirs = new HashSet<>();
	public final Map<String, PathData<T>> dirMap = new HashMap<>();

	protected long size;

	public PathData(final T dir) {
		this.dir = dir;
	}

	public void push(final String name, final T path, final BasicFileAttributes attr) {
		this.size += attr.size();

		this.push(name, path);
	}

	protected final void push(final String name, final T path) {
		this.paths.add(path);

		if (name != null) {
			pathMap.put(name, path);
		}
	}

	public void push(final String name, final PathData<T> dir) {
		this.dirs.add(dir);

		if (name != null) {
			dirMap.put(name, dir);
		}
	}

	public PathData<T> resolveDir(final String name) {
		return this.dirMap.get(name);
	}

	public T resolvePath(final String name) {
		return this.pathMap.get(name);
	}

	@Override
	public boolean equals(final Object obj) {
		return obj == this || obj instanceof PathData<?> other && this.dir.equals(other.dir);
	}

	@Override
	public int hashCode() {
		return this.dir.hashCode();
	}

	@Override
	public String toString() {
		return "PathData(" + dir + "){" +
				 "paths=" + paths +
				 ", dirs=" + dirs +
				 ", size=" + size +
				 '}';
	}
}
