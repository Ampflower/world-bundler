package gay.ampflower.bundler.utils.function;

/**
 * @author Ampflower
 * @since ${version}
 **/
@FunctionalInterface
public interface IntBiFunction<R> {
	R apply(int i1, int i2);
}
