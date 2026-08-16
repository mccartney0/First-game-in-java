package com.traduvertgames.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser JSON mínimo (sem dependências externas) que lê o formato gerado por
 * {@link JsonWriter}. Suporta objetos, arrays, strings, números inteiros e
 * decimais, e os literais true/false/null.
 */
final class JsonParser {

	private final String input;
	private int position;

	private JsonParser(String input) {
		this.input = input;
		this.position = 0;
	}

	static Object parse(String input) {
		JsonParser parser = new JsonParser(input == null ? "" : input);
		Object result = parser.readValue();
		parser.skipWhitespace();
		return result;
	}

	private void skipWhitespace() {
		while (position < input.length() && Character.isWhitespace(input.charAt(position))) {
			position++;
		}
	}

	private char peek() {
		skipWhitespace();
		if (position >= input.length()) {
			return 0;
		}
		return input.charAt(position);
	}

	private char next() {
		char current = input.charAt(position);
		position++;
		return current;
	}

	private void expect(char expected) {
		char actual = peek();
		if (actual != expected) {
			return;
		}
		next();
	}

	private Object readValue() {
		char token = peek();
		if (token == '{') {
			return readObject();
		}
		if (token == '[') {
			return readArray();
		}
		if (token == '"') {
			return readString();
		}
		if (token == '-' || Character.isDigit(token)) {
			return readNumber();
		}
		if (input.startsWith("true", position)) {
			position += 4;
			return Boolean.TRUE;
		}
		if (input.startsWith("false", position)) {
			position += 5;
			return Boolean.FALSE;
		}
		if (input.startsWith("null", position)) {
			position += 4;
			return null;
		}
		return null;
	}

	private Map<String, Object> readObject() {
		Map<String, Object> map = new HashMap<String, Object>();
		next(); // '{'
		skipWhitespace();
		if (peek() == '}') {
			next();
			return map;
		}
		while (true) {
			skipWhitespace();
			String key = readString();
			skipWhitespace();
			expect(':');
			skipWhitespace();
			Object value = readValue();
			if (key != null) {
				map.put(key, value);
			}
			skipWhitespace();
			char delimiter = peek();
			if (delimiter == ',') {
				next();
			} else {
				break;
			}
		}
		expect('}');
		return map;
	}

	private List<Object> readArray() {
		List<Object> list = new ArrayList<Object>();
		next(); // '['
		skipWhitespace();
		if (peek() == ']') {
			next();
			return list;
		}
		while (true) {
			skipWhitespace();
			list.add(readValue());
			skipWhitespace();
			char delimiter = peek();
			if (delimiter == ',') {
				next();
			} else {
				break;
			}
		}
		expect(']');
		return list;
	}

	private String readString() {
		if (peek() != '"') {
			return null;
		}
		next(); // '"'
		StringBuilder builder = new StringBuilder();
		while (position < input.length()) {
			char current = input.charAt(position);
			if (current == '"') {
				position++;
				return builder.toString();
			}
			if (current == '\\' && position + 1 < input.length()) {
				char escaped = input.charAt(position + 1);
				switch (escaped) {
				case '"':
					builder.append('"');
					break;
				case '\\':
					builder.append('\\');
					break;
				case 'n':
					builder.append('\n');
					break;
				case 'r':
					builder.append('\r');
					break;
				case 't':
					builder.append('\t');
					break;
				default:
					builder.append(escaped);
					break;
				}
				position += 2;
			} else {
				builder.append(current);
				position++;
			}
		}
		return builder.toString();
	}

	private Number readNumber() {
		int start = position;
		if (peek() == '-') {
			position++;
		}
		while (position < input.length() && Character.isDigit(input.charAt(position))) {
			position++;
		}
		boolean decimal = false;
		if (position < input.length() && input.charAt(position) == '.') {
			decimal = true;
			position++;
			while (position < input.length() && Character.isDigit(input.charAt(position))) {
				position++;
			}
		}
		String token = input.substring(start, position);
		try {
			if (decimal || token.contains(".")) {
				return Double.parseDouble(token);
			}
			long longValue = Long.parseLong(token);
			if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
				return (int) longValue;
			}
			return longValue;
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
