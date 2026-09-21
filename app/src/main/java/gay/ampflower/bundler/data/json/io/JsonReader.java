package gay.ampflower.bundler.data.json.io;

import it.unimi.dsi.fastutil.chars.Char2CharMap;
import it.unimi.dsi.fastutil.chars.Char2CharOpenHashMap;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.CharBuffer;
import java.util.Arrays;

/**
 * @author Ampflower
 * @since ${version}
 **/
public class JsonReader implements AutoCloseable {
	private static final char[] whitespace = {' ', '\t', '\n', 0, 0, '\r', 0, 0};
	private static final char[] rawTrue = {'t', 'r', 'u', 'e'};
	private static final char[] rawFalse = {'f', 'a', 'l', 's', 'e'};
	private static final char[] rawNull = {'n', 'u', 'l', 'l'};
	private static final Char2CharMap escapeMapper;
	private static final int HEX_RADIX = 16;

	static {
		final var escapes = new Char2CharOpenHashMap();
		escapes.put('"', '"');
		escapes.put('\\', '\\');
		escapes.put('/', '/');
		escapes.put('b', '\b');
		escapes.put('f', '\f');
		escapes.put('n', '\n');
		escapes.put('r', '\r');
		escapes.put('t', '\t');
		escapes.put('u', '\uFFFF');
		escapes.trim();
		escapeMapper = escapes;
	}

	private final Reader reader;
	private final char[] buf = new char[8192];
	private final CharBuffer nio = CharBuffer.wrap(buf);

	private int ia, ib;

	private final StringBuilder builder = new StringBuilder(4096);
	// number buffer; if *anyone* needs more than this, uh, please, stop
	private final char[] num = new char[1024];

	public JsonReader(final Reader reader) {
		this.reader = reader;
	}

	private void available(int required) throws IOException {
		if (ib - ia < required) {
			alignAndRead();
		}
		if (ib < required) {
			throw new IOException("Not enough available: required " + required + " chars");
		}
	}

	private boolean primeBuffer() throws IOException {
		if (ia == ib) {
			ia = 0;
			ib = readNChars(reader, buf, 0, buf.length);
		}
		return ib != 0;
	}

	private boolean assertivePrimeBuffer(char falseIf) throws IOException {
		if (!primeBuffer()) {
			throw new IOException("EOF");
		}
		if (buf[ia] != falseIf) {
			return true;
		}
		return false;
	}

	private void alignAndRead() throws IOException {
		final int len = ib - ia;
		System.arraycopy(buf, ia, buf, 0, len);
		ib = len + readNChars(reader, buf, len, buf.length - len);
		ia = 0;
	}

	private static int readNChars(
		final Reader reader,
		final char[] buf,
		final int offset,
		final int length
	) throws IOException {
		if (offset + length > buf.length) {
			throw new IllegalArgumentException("expected read exceeds buffer size");
		}

		int count = 0;

		while (count < length) {
			final int read = reader.read(buf, offset + count, length - count);
			if (read < 0) {
				break;
			}
			count += read;
		}

		return count;
	}

	private void skipWhitespace() throws IOException {
		while (primeBuffer()) {
			while (ia < ib) {
				char c = buf[ia];
				if (whitespace[(c & 7)] != c) {
					return;
				}
				ia++;
			}
		}
	}

	private void readTrue() throws IOException {
		matchBuffer(rawTrue);
	}

	private void readFalse() throws IOException {
		matchBuffer(rawFalse);
	}

	private void readNull() throws IOException {
		matchBuffer(rawNull);
	}

	private BigDecimal readNumber() throws IOException {
		int in = 0;
		boolean started = false, seenExponent = false, startedExponent = false;
		loop:
		while (primeBuffer()) {
			char c = buf[ia++];

			switch (c) {
				case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
					num[in++] = c;
				}
				case '+', '-' -> {
					if (started & (!seenExponent | startedExponent)) {
						throw new IOException("Invalid sign placement: " + c);
					}
					num[in++] = c;
				}
				case 'e', 'E' -> {
					if (!started || seenExponent) {
						throw new IOException("Invalid exponent placement: " + c);
					}
					seenExponent = true;
					num[in++] = c;

					continue;
				}
				case ' ', '\t', '\n', '\r', ',', ']', '}' -> {
					ia--;
					break loop;
				}
				default -> throw new IOException("Not a digit: " + c);
			}

			started = true;
			if (seenExponent) {
				startedExponent = true;
			}
		}

		return new BigDecimal(num, 0, in);
	}

	private String readString() throws IOException {
		if (buf[ia++] != '"') {
			throw new IOException("not a string");
		}

		final var builder = this.builder;
		builder.delete(0, builder.length());

		do {
			if (!assertivePrimeBuffer('"')) {
				break;
			}

			char c = buf[ia++];

			if (c != '\\') {
				builder.append(c);
				continue;
			}

			available(1);

			var value = escapeMapper.get(buf[ia++]);

			if (value == 0) {
				throw new IOException("invalid escape: \\" + c);
			}

			if (value != '\uFFFF') {
				builder.append(value);
				continue;
			}

			available(4);

			char point = (char) Integer.parseUnsignedInt(nio, ia, ia + 4, HEX_RADIX);

			ia += 4;

			builder.append(point);

			if (!primeBuffer()) {
				throw new IOException("EOF");
			}

		} while (assertivePrimeBuffer('"'));

		ia++;

		return builder.toString();
	}


	private void matchBuffer(final char[] expected) throws IOException {
		available(expected.length);
		if (!Arrays.equals(this.buf, ia, ia + expected.length, expected, 0, expected.length)) {
			throw new IOException("Not " + String.valueOf(expected));
		}
		ia += expected.length;
	}

	private Type senseType() throws IOException {
		skipWhitespace();
		primeBuffer();
		if (ia == ib) {
			return Type.EOF;
		}
		return switch (buf[ia]) {
			case '{' -> Type.OBJECT_START;
			case '}' -> Type.OBJECT_END;
			case '[' -> Type.ARRAY_START;
			case ']' -> Type.ARRAY_END;
			case '.' -> Type.FRACTION;
			case ',' -> Type.CONTINUE;
			case '"' -> Type.STRING;
			case ':' -> Type.VALUE;
			case 'e', 'E' -> Type.EXPONENT;
			case 'f' -> Type.FALSE;
			case 't' -> Type.TRUE;
			case 'n' -> Type.NULL;
			case '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> Type.NUMBER;
			default -> Type.INVALID;
		};
	}

	private Type step() throws IOException {
		ia++;
		return senseType();
	}

	public void parse(final SaxJsonParser parser) throws IOException {
		var type = senseType();

		if (type == Type.EOF) {
			return;
		}

		jump(parser, type);

		type = senseType();

		if (type != Type.EOF) {
			throw new IOException("Corrupted JSON file? -> " + type);
		}
	}

	private void jump(final SaxJsonParser parser, final Type type) throws IOException {
		switch (type) {
			case NULL -> {
				this.readNull();
				parser.ofNull();
			}
			case FALSE -> {
				this.readFalse();
				parser.ofBoolean(false);
			}
			case TRUE -> {
				this.readTrue();
				parser.ofBoolean(true);
			}
			case STRING -> parser.ofString(this.readString());
			case NUMBER -> parser.ofNumber(this.readNumber());
			case ARRAY_START -> this.parseList(parser);
			case OBJECT_START -> this.parseCompound(parser);
			default -> throw new IllegalStateException();
		}
	}

	private void parseList(final SaxJsonParser parser) throws IOException {
		parser.startList();

		Type type = step();

		if (type == Type.ARRAY_END) {
			parser.endTag();
			return;
		}

		if (!type.valid) {
			throw new IOException("invalid sense within array: " + type);
		}

		while (true) {
			jump(parser, type);

			type = senseType();
			if (type == Type.ARRAY_END) {
				ia++;
				parser.endTag();
				return;
			}

			if (type != Type.CONTINUE) {
				throw new IOException("unexpected token: " + type);
			}

			type = step();
			if (type.terminating) {
				throw new IOException("comma before ending bracket");
			}
		}
	}

	private void parseCompound(final SaxJsonParser parser) throws IOException {
		parser.startCompound();

		Type type = step();

		if (type == Type.OBJECT_END) {
			ia++;
			parser.endTag();
			return;
		}

		if (type != Type.STRING) {
			throw new IOException("unexpected token: " + type);
		}

		while (true) {
			parser.field(this.readString());

			if ((type = senseType()) != Type.VALUE) {
				throw new IOException("unexpected token: " + type);
			}

			if (!(type = step()).valid) {
				throw new IOException("invalid sense within object: " + type);
			}

			jump(parser, type);

			type = senseType();

			if (type == Type.OBJECT_END) {
				ia++;
				parser.endTag();
				return;
			}

			if (type != Type.CONTINUE) {
				throw new IOException("unexpected token: " + type);
			}

			type = step();
			if (type.terminating) {
				throw new IOException("comma before ending bracket");
			}

			if (type != Type.STRING) {
				throw new IOException("unexpected token: " + type);
			}
		}
	}

	@Override
	public void close() throws IOException {
		ia = ib = 0;
		this.reader.close();
	}

	private enum Type {
		EOF(false, false, true),
		INVALID(false, false, false),
		NULL(true, false, false),
		FALSE(true, false, false),
		TRUE(true, false, false),
		STRING(true, false, false),
		NUMBER(true, false, false),
		FRACTION(false, false, false),
		EXPONENT(false, false, false),
		ARRAY_START(true, true, false),
		ARRAY_END(false, false, true),
		OBJECT_START(true, true, false),
		OBJECT_END(false, false, true),
		VALUE(false, false, false),
		CONTINUE(false, false, false),
		;

		private final boolean valid, starting, terminating;

		Type(final boolean valid, final boolean starting, final boolean terminating) {
			this.valid = valid;
			this.starting = starting;
			this.terminating = terminating;
		}
	}
}
