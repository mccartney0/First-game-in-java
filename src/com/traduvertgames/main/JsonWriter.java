package com.traduvertgames.main;

import java.util.List;
import java.util.Map;

/**
 * Serializador JSON mínimo (sem dependências externas) que gera o formato
 * lido por {@link JsonParser}, usado pelo {@link SaveManager}.
 */
final class JsonWriter {

	private static final String INDENT = "  ";

	private JsonWriter() {
	}

	static String write(Object value) {
		StringBuilder builder = new StringBuilder();
		append(builder, value, 0);
		builder.append('\n');
		return builder.toString();
	}

	@SuppressWarnings("unchecked")
	private static void append(StringBuilder builder, Object value, int depth) {
		if (value instanceof Map) {
			writeObject(builder, (Map<String, Object>) value, depth);
		} else if (value instanceof List) {
			writeArray(builder, (List<?>) value, depth);
		} else if (value instanceof String) {
			writeString(builder, (String) value);
		} else if (value instanceof Boolean) {
			builder.append(value);
		} else if (value instanceof Number) {
			writeNumber(builder, (Number) value);
		} else if (value == null) {
			builder.append("null");
		} else {
			writeString(builder, value.toString());
		}
	}

	private static void writeObject(StringBuilder builder, Map<String, Object> map, int depth) {
		builder.append('{');
		boolean first = true;
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			if (entry.getValue() == null) {
				continue;
			}
			if (!first) {
				builder.append(',');
			}
			builder.append('\n');
			for (int i = 0; i < depth + 1; i++) {
				builder.append(INDENT);
			}
			writeString(builder, entry.getKey());
			builder.append(':');
			append(builder, entry.getValue(), depth + 1);
			first = false;
		}
		if (!first) {
			builder.append('\n');
			for (int i = 0; i < depth; i++) {
				builder.append(INDENT);
			}
		}
		builder.append('}');
	}

	private static void writeArray(StringBuilder builder, List<?> list, int depth) {
		builder.append('[');
		for (int i = 0; i < list.size(); i++) {
			if (i > 0) {
				builder.append(',');
			}
			builder.append('\n');
			for (int j = 0; j < depth + 1; j++) {
				builder.append(INDENT);
			}
			append(builder, list.get(i), depth + 1);
		}
		if (!list.isEmpty()) {
			builder.append('\n');
			for (int i = 0; i < depth; i++) {
				builder.append(INDENT);
			}
		}
		builder.append(']');
	}

	private static void writeString(StringBuilder builder, String text) {
		builder.append('"');
		for (int i = 0; i < text.length(); i++) {
			char current = text.charAt(i);
			switch (current) {
			case '"':
				builder.append("\\\"");
				break;
			case '\\':
				builder.append("\\\\");
				break;
			case '\n':
				builder.append("\\n");
				break;
			case '\r':
				builder.append("\\r");
				break;
			case '\t':
				builder.append("\\t");
				break;
			default:
				builder.append(current);
				break;
			}
		}
		builder.append('"');
	}

	private static void writeNumber(StringBuilder builder, Number number) {
		if (number instanceof Double || number instanceof Float) {
			double value = number.doubleValue();
			if (value == Math.floor(value) && !Double.isInfinite(value)) {
				builder.append(String.valueOf((long) value)).append(".0");
			} else {
				builder.append(String.valueOf(value));
			}
		} else {
			builder.append(String.valueOf(number.longValue()));
		}
	}
}
