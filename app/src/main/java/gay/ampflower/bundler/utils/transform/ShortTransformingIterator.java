package gay.ampflower.bundler.utils.transform;

import it.unimi.dsi.fastutil.shorts.Short2ObjectFunction;
import it.unimi.dsi.fastutil.shorts.ShortIterator;

import java.util.Iterator;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class ShortTransformingIterator<O> implements Iterator<O> {
	private final ShortIterator input;
	private final Short2ObjectFunction<O> transformer;

	public ShortTransformingIterator(ShortIterator input, Short2ObjectFunction<O> transformer) {
		this.input = input;
		this.transformer = transformer;
	}

	@Override
	public boolean hasNext() {
		return input.hasNext();
	}

	@Override
	public O next() {
		return transformer.apply(input.nextShort());
	}
}
