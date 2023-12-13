package gay.ampflower.bundler.world.io.dir;

import gay.ampflower.bundler.utils.SizeUtils;

import java.util.List;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class DirectoryMeta {
	public List<DirectoryData> list;
	public long size;
	public long dirs;
	public long files;
	public long error;

	@Override
	public String toString() {
		return "DirectoryMeta{" +
			"size=" + SizeUtils.displaySize(size) +
			", dirs=" + dirs +
			", files=" + files +
			", error=" + error +
			'}';
	}
}
