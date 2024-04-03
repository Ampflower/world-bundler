package gay.ampflower.bundler.utils.transform;

import it.unimi.dsi.fastutil.ints.IntIterator;

import java.util.Iterator;
import java.util.function.IntFunction;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class IntTransformingIterator<O> implements Iterator<O> {
	private final IntIterator input;
	private final IntFunction<O> transformer;

	public IntTransformingIterator(IntIterator input, IntFunction<O> transformer) {
		this.input = input;
		this.transformer = transformer;
	}

	@Override
	public boolean hasNext() {
		return input.hasNext();
	}

	@Override
	public O next() {
		return transformer.apply(input.nextInt());
	}
}
