package gay.ampflower.bundler.utils;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class SysProps {
	private static final StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

	public static final boolean DEBUG = Boolean.getBoolean("worldbunder.debug");

	public static boolean isDebuggee() {
		return DEBUG || walker.getCallerClass().desiredAssertionStatus();
	}
}
