package gay.ampflower.bundler.world.io.dir;

import gay.ampflower.bundler.utils.SizeUtils;

import java.util.List;
import java.util.Map;

/**
 * @author Ampflower
 * @since ${version}
 **/
public record DirectoryMeta<T extends PathData<?>>(
	List<T> visited,
	Map<String, List<T>> poi,
	long size,
	long dirs,
	long files,
	long error
) {

	public DirectoryMeta(List<T> visited, long size, long dirs, long files, long error) {
		this(visited, Map.of(), size, dirs, files, error);
	}

	@Override
	public String toString() {
		return "DirectoryMeta{" +
				 "visited=" + visited +
				 ", poi=" + poi +
				 ", size=" + SizeUtils.displaySize(size) +
				 ", dirs=" + dirs +
				 ", files=" + files +
				 ", error=" + error +
				 '}';
	}
}
