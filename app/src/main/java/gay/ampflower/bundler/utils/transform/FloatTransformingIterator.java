package gay.ampflower.bundler.utils.transform;

import it.unimi.dsi.fastutil.floats.Float2ObjectFunction;
import it.unimi.dsi.fastutil.floats.FloatIterator;

import java.util.Iterator;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class FloatTransformingIterator<O> implements Iterator<O> {
	private final FloatIterator input;
	private final Float2ObjectFunction<O> transformer;

	public FloatTransformingIterator(FloatIterator input, Float2ObjectFunction<O> transformer) {
		this.input = input;
		this.transformer = transformer;
	}

	@Override
	public boolean hasNext() {
		return input.hasNext();
	}

	@Override
	public O next() {
		return transformer.get(input.nextFloat());
	}
}
