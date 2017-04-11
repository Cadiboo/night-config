package com.electronwill.nightconfig.toml;

import com.electronwill.nightconfig.core.serialization.CharacterInput;
import com.electronwill.nightconfig.core.serialization.CharsWrapper;
import com.electronwill.nightconfig.core.serialization.ParsingException;
import com.electronwill.nightconfig.core.serialization.Utils;

/**
 * @author TheElectronWill
 * @see <a href="https://github.com/toml-lang/toml#user-content-integer">TOML specification - Integers</a>
 * @see <a href="https://github.com/toml-lang/toml#user-content-float">TOML specification - Floats</a>
 * @see <a href="https://github.com/toml-lang/toml#user-content-boolean">TOML specification - Booleans</a>
 */
final class ValueParser {

	private static final char[] END_OF_VALUE = {'\t', ' ', '\n', '\r', ',', ']'};
	private static final char[] TRUE_END = {'r', 'u', 'e'}, FALSE_END = {'a', 'l', 's', 'e'};
	private static final char[] ONLY_IN_FP_NUMBER = {'.', 'e', 'E'};

	/**
	 * Parses a TOML value. The value's type is determinated with the first character, and with
	 * the next ones if necessary.
	 */
	static Object parse(CharacterInput input, char firstChar, TomlParser parser) {
		switch (firstChar) {
			case '{':
				return TableParser.parseInline(input, parser);
			case '[':
				return ArrayParser.parse(input, parser);
			case '\'':
				if (input.peek() == '\'' && input.peek(1) == '\'') {
					input.skipPeeks();// Don't include the opening quotes in the String
					return StringParser.parseMultiLiteral(input, parser);
				}
				return StringParser.parseLiteral(input, parser);
			case '\"':
				if (input.peek() == '\"' && input.peek(1) == '\"') {
					input.skipPeeks();// Don't include the opening quotes in the String
					return StringParser.parseMultiBasic(input, parser);
				}
				return StringParser.parseBasic(input, parser);
			case 't':
				return parseTrue(input);
			case 'f':
				return parseFalse(input);
			case '+':
			case '-':
				input.pushBack(firstChar);
				return parseNumber(input);
			default:
				input.pushBack(firstChar);
				return parseNumberOrDateTime(input);
		}
	}

	static Object parse(CharacterInput input, TomlParser parser) {
		return parse(input, Toml.readNonSpaceChar(input, false), parser);
	}

	private static Object parseNumberOrDateTime(CharacterInput input) {
		CharsWrapper valueChars = input.readUntil(END_OF_VALUE);
		if (TemporalParser.shouldBeTemporal(valueChars)) {
			return TemporalParser.parse(valueChars);
		}
		return parseNumber(valueChars);
	}

	private static Number parseNumber(CharacterInput input) {
		CharsWrapper valueChars = input.readUntil(END_OF_VALUE);
		return parseNumber(valueChars);
	}

	private static Number parseNumber(CharsWrapper valueChars) {
		valueChars = simplifyNumber(valueChars);
		if (valueChars.indexOfFirst(ONLY_IN_FP_NUMBER) != -1) {
			return Utils.parseDouble(valueChars);
		}
		long longValue = Utils.parseLong(valueChars, 10);
		int intValue = (int)longValue;
		if (intValue == longValue) {
			return intValue;// returns an int if it is enough to represent the value correctly
		}
		return longValue;
	}

	private static CharsWrapper simplifyNumber(CharsWrapper numberChars) {
		if (numberChars.charAt(0) == '_') {
			throw new ParsingException("Invalid leading underscore in number " + numberChars);
		}
		if (numberChars.charAt(numberChars.length() - 1) == '_') {
			throw new ParsingException("Invalid trailing underscore in number " + numberChars);
		}
		CharsWrapper.Builder builder = new CharsWrapper.Builder(16);
		boolean nextCannotBeUnderscore = false;
		for (char c : numberChars) {
			if (c == '_') {
				if (nextCannotBeUnderscore) {
					throw new ParsingException("Invalid underscore followed by another one in "
											   + "number "
											   + numberChars);
				} else {
					nextCannotBeUnderscore = true;
				}
			} else {
				if (nextCannotBeUnderscore) {
					nextCannotBeUnderscore = false;
				}
				builder.append(c);
			}
		}
		return builder.build();
	}

	private static Boolean parseFalse(CharacterInput input) {
		CharsWrapper remaining = input.readUntil(END_OF_VALUE);
		if (!remaining.contentEquals(FALSE_END)) {
			throw new ParsingException(
					"Invalid value f" + remaining + " - Expected the boolean value false.");
		}
		return false;
	}

	private static Boolean parseTrue(CharacterInput input) {
		CharsWrapper remaining = input.readUntil(END_OF_VALUE);
		if (!remaining.contentEquals(TRUE_END)) {
			throw new ParsingException(
					"Invalid value t" + remaining + " - Expected the boolean value true.");
		}
		return true;
	}

	private ValueParser() {}
}