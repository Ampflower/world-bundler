package gay.ampflower.bundler.utils.transform;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectFunction;
import it.unimi.dsi.fastutil.bytes.ByteIterator;

import java.util.Iterator;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class ByteTransformingIterator<O> implements Iterator<O> {
	private final ByteIterator input;
	private final Byte2ObjectFunction<O> transformer;

	public ByteTransformingIterator(ByteIterator input, Byte2ObjectFunction<O> transformer) {
		this.input = input;
		this.transformer = transformer;
	}

	@Override
	public boolean hasNext() {
		return input.hasNext();
	}

	@Override
	public O next() {
		return transformer.apply(input.nextByte());
	}
}
