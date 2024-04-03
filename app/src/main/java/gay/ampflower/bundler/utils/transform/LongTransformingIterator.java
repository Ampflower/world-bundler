package gay.ampflower.bundler.utils.transform;

import it.unimi.dsi.fastutil.longs.LongIterator;

import java.util.Iterator;
import java.util.function.LongFunction;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class LongTransformingIterator<O> implements Iterator<O> {
	private final LongIterator input;
	private final LongFunction<O> transformer;

	public LongTransformingIterator(LongIterator input, LongFunction<O> transformer) {
		this.input = input;
		this.transformer = transformer;
	}

	@Override
	public boolean hasNext() {
		return input.hasNext();
	}

	@Override
	public O next() {
		return transformer.apply(input.nextLong());
	}
}
