package gay.ampflower.bundler.utils;

import gay.ampflower.bundler.utils.io.PathUtils;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Functionally an implementation of the XDG specification and OS utilities.
 * <p>
 * If any corresponding XDG environment is specified,
 * it shall take priority over any other environment to respect user choice.
 * <p>
 * Any variables where left undefined may use platform-specific directories
 * to ensure intuition for whomever uses the software.
 *
 * @author Ampflower
 * @since ${version}
 **/
public final class PlatformUtils {
	private static final Logger logger = LogUtils.logger();

	public static final Path HOME;

	static {
		String home = System.getenv("HOME");
		if (home == null) {
			home = System.getProperty("user.home");
		}
		if (home == null) {
			logger.warn("Setting HOME to \"{}\".", PathUtils.normalized("."));
			home = ".";
		}
		HOME = PathUtils.normalized(home);
	}

	public static final Path XDG_DATA_HOME = resolveWithFallback(
		"XDG_DATA_HOME",
		"AppData",
		"Application Support",
		".local/share"
	);
	public static final Path XDG_CONFIG_HOME = resolveWithFallback(
		"XDG_CONFIG_HOME",
		"AppData",
		"Application Support",
		".config"
	);
	public static final Path XDG_STATE_HOME = resolveWithFallback(
		"XDG_STATE_HOME",
		"LocalAppData",
		null,
		".local/state"
	);
	public static final List<Path> XDG_DATA_DIRS = resolveWithFallback(
		"XDG_DATA_DIRS",
		"/usr/local/share/",
		"/usr/share/"
	);
	public static final List<Path> XDG_CONFIG_DIRS = resolveWithFallback("XDG_CONFIG_DIRS", "/etc/xdg");
	public static final Path XDG_CACHE_HOME = resolveWithFallback("XDG_CACHE_HOME", "LocalAppData", "Caches", ".cache");
	public static final Path XDG_RUNTIME_DIR = tryResolve("XDG_RUNTIME_DIR", true, true);

	static {
		if (SysProps.isDebuggee()) {
			printDiagnostics();
		}
	}

	@CheckReturnValue
	public static List<Path> findConfigs(
		final boolean followDotD,
		final boolean followDirectoryOmittingExtension,
		final boolean traverseData,
		final boolean traverseHome,
		final boolean recursive,
		final String... names
	) {
		final var list = new ArrayList<Path>();

		for (final var name : names) {
			findConfigs(
				list,
				followDotD,
				followDirectoryOmittingExtension,
				traverseData,
				traverseHome,
				recursive,
				Path.of(name)
			);
		}

		return list;
	}

	public static boolean findConfigs(
		final Collection<? super Path> paths,
		final boolean followDotD,
		final boolean followDirectoryOmittingExtension,
		final boolean traverseData,
		final boolean traverseHome,
		final boolean recursive,
		final Path path
	) {
		if (path.isAbsolute()) {
			// Nothing to do.
			return paths.add(path);
		}

		boolean flag = false;

		if (traverseHome) {
			flag = findConfigs(paths, followDotD, followDirectoryOmittingExtension, recursive, HOME, path);
		}

		if (traverseData) {
			if (findConfigs(paths, followDotD, followDirectoryOmittingExtension, recursive, XDG_DATA_HOME, path)) {
				flag = true;
			} else {
				for (final var data : XDG_DATA_DIRS) {
					flag |= findConfigs(paths, followDotD, followDirectoryOmittingExtension, recursive, data, path);
				}
			}
		}

		return flag;
	}

	@CheckReturnValue
	private static boolean findConfigs(
		final Collection<? super Path> paths,
		final boolean followDotD,
		final boolean followDirectoryOmittingExtension,
		final boolean recursive,
		final Path root,
		final Path path
	) {
		if (!PathUtils.isReadableDirectory(root)) {
			return false;
		}

		boolean flag = false;

		final Path resolved = root.resolve(path);
		if (PathUtils.isReadableRegular(resolved)) {
			flag |= paths.add(resolved);
		} else if (followDirectoryOmittingExtension) {
			flag |= findConfigs(paths, recursive, omitExtension(path));
		}

		if (followDotD) {
			flag |= findConfigs(paths, recursive, dotD(path));
		}

		return flag;
	}

	@CheckReturnValue
	private static boolean findConfigs(
		final Collection<? super Path> paths,
		final boolean recursive,
		final Path path
	) {
		if (Files.notExists(path)) {
			return false;
		}
		if (!Files.isDirectory(path)) {
			return Files.isRegularFile(path) && paths.add(path);
		}

		try {
			final var result = Files.walk(path, recursive ? Integer.MAX_VALUE : 1, FileVisitOption.FOLLOW_LINKS)
				.filter(Files::isRegularFile)
				.toList();
			return paths.addAll(result);
		} catch (IOException io) {
			logger.warn("Unexpected error walking {}, ignoring", path, io);
		}

		return false;
	}

	@CheckReturnValue
	private static Path omitExtension(final Path path) {
		final String filename = path.getFileName().toString();
		final int extension = filename.lastIndexOf('.');
		if (extension < 0) {
			return path;
		}
		return resolveRelativeToParent(path, filename.substring(0, extension));
	}

	@CheckReturnValue
	private static Path dotD(final Path path) {
		return resolveRelativeToParent(path, path.getFileName() + ".d");
	}

	@CheckReturnValue
	private static Path resolveRelativeToParent(final Path path, final String subpath) {
		final Path candidate = Path.of(subpath);
		if (candidate.isAbsolute()) {
			throw new IllegalArgumentException("subpath is to not be absolute: " + subpath);
		}
		final Path parent = path.getParent();
		return parent != null ? path.resolve(candidate) : candidate;
	}

	private static List<Path> resolveWithFallback(
		final String xdg,
		final String... unix
	) {
		final var list = new ArrayList<Path>();
		primary:
		{
			if (xdg == null) {
				break primary;
			}

			final String value = System.getenv(xdg);

			if (value == null) {
				break primary;
			}

			for (final String raw : value.split("\\" + File.pathSeparatorChar)) {
				if (raw.isEmpty()) {
					continue;
				}

				final Path path = Path.of(raw);

				if (!path.isAbsolute()) {
					continue;
				}

				list.add(path.normalize());
			}

			if (!list.isEmpty()) {
				return List.copyOf(list);
			}
		}

		for (final String raw : unix) {
			final Path path = Path.of(raw).normalize();
			list.add(path.isAbsolute() ? path : HOME.resolve(path));
		}

		return List.copyOf(list);
	}

	private static Path resolveWithFallback(
		final String xdg,
		final String windows,
		final String macintosh,
		final String unix
	) {
		Path path = tryResolve(xdg, true, false);

		if (path == null && windows != null) {
			path = tryResolve(windows, true, true);
		}

		if (path == null && macintosh != null) {
			path = tryResolve(macintosh, true, true);
		}

		if (path == null && unix != null) {
			path = Path.of(unix).normalize();
			path = path.isAbsolute() ? path : HOME.resolve(path);
		}

		return path;
	}

	private static @Nullable Path tryResolve(
		final String environment,
		final boolean avoidRelative,
		final boolean mustExist
	) {
		if (environment == null) {
			return null;
		}

		final String value = System.getenv(environment);
		if (value == null) {
			return null;
		}

		final Path path = Path.of(environment);
		if (avoidRelative && !path.isAbsolute()) {
			return null;
		}

		if (mustExist && !Files.exists(path)) {
			return null;
		}

		return path.toAbsolutePath().normalize();
	}

	public static void printDiagnostics() {
		logger.debug("HOME: {}", HOME);
		logger.debug("XDG_DATA_HOME: {}", XDG_DATA_HOME);
		logger.debug("XDG_CONFIG_HOME: {}", XDG_CONFIG_HOME);
		logger.debug("XDG_STATE_HOME: {}", XDG_STATE_HOME);
		logger.debug("XDG_DATA_DIRS: {}", XDG_DATA_DIRS);
		logger.debug("XDG_CONFIG_DIRS: {}", XDG_CONFIG_DIRS);
		logger.debug("XDG_CACHE_HOME: {}", XDG_CACHE_HOME);
		logger.debug("XDG_RUNTIME_DIR: {}", XDG_RUNTIME_DIR);
	}
}
