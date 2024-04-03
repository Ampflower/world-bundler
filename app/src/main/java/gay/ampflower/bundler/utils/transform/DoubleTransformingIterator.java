package gay.ampflower.bundler.utils.transform;

import it.unimi.dsi.fastutil.doubles.DoubleIterator;

import java.util.Iterator;
import java.util.function.DoubleFunction;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class DoubleTransformingIterator<O> implements Iterator<O> {
	private final DoubleIterator input;
	private final DoubleFunction<O> transformer;

	public DoubleTransformingIterator(DoubleIterator input, DoubleFunction<O> transformer) {
		this.input = input;
		this.transformer = transformer;
	}

	@Override
	public boolean hasNext() {
		return input.hasNext();
	}

	@Override
	public O next() {
		return transformer.apply(input.nextDouble());
	}
}
