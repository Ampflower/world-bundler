package gay.ampflower.bundler.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ampflower
 * @since ${version}
 **/
public final class LogUtils {
	private static final StackWalker walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

	/**
	 * Fetches a logger for the calling class.
	 * */
	public static Logger logger() {
		return LoggerFactory.getLogger(walker.getCallerClass());
	}
}
