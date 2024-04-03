package gay.ampflower.bundler.utils.transform;

import java.util.Iterator;
import java.util.function.Function;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class Transiterator<I, O> implements Iterator<O> {
	private final Iterator<I> input;
	private final Function<I, O> transformer;

	public Transiterator(Iterator<I> input, Function<I, O> transformer) {
		this.input = input;
		this.transformer = transformer;
	}

	@Override
	public boolean hasNext() {
		return input.hasNext();
	}

	@Override
	public O next() {
		return transformer.apply(input.next());
	}
}
