package nlScript.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class Json {

	public static abstract class JsonThing {
		public abstract String toJson(String indent);
	}

	public static class JsonArray extends JsonThing implements Iterable<JsonThing> {
		final ArrayList<JsonThing> elements = new ArrayList<>();

		public void add(JsonThing element) {
			elements.add(element);
		}

		public void addIfNotNull(JsonThing element) {
			if(element != null)
				add(element);
		}

		public int size() {
			return elements.size();
		}

		public JsonThing get(int i) {
			return elements.get(i);
		}

		@Override
		public Iterator<JsonThing> iterator() {
			return elements.iterator();
		}

		public String toJson(String indent) {
			StringBuilder ret = new StringBuilder();
			ret.append("[\n");
			indent += "  ";
			for(JsonThing e : elements) {
				ret.append(indent).append(e.toJson(indent)).append(",\n");
			}
			removeTrailingComma(ret);
			indent = indent.substring(2);
			ret.append("\n").append(indent).append("]");
			return ret.toString();
		}
	}

	public static class JsonObject extends JsonThing {
		final HashMap<String, JsonThing> properties = new HashMap<>();

		public void put(String key, JsonThing value) {
			if(properties.containsKey(key))
				throw new RuntimeException(key + " already exists: " + properties);
			properties.put(key, value);
		}

		public void putIfNotNull(String key, JsonThing element) {
			if(element != null)
				put(key, element);
		}

		public Set<String> getPropertyNames() {
			return properties.keySet();
		}

		public JsonThing get(String propertyName) {
			return properties.get(propertyName);
		}

		public String getAsString(String propertyName) {
			return ((JsonString) properties.get(propertyName)).value;
		}

		public String toJson(String indent) {
			StringBuilder ret = new StringBuilder();
			ret.append("{\n");
			indent += "  ";
			for(String key : properties.keySet()) {
				JsonThing e = properties.get(key);
				ret.append(indent).append('"').append(key).append('"').append(": ").append(e.toJson(indent)).append(",\n");
			}
			removeTrailingComma(ret);
			indent = indent.substring(2);
			ret.append("\n").append(indent).append("}");
			return ret.toString();
		}
	}

	public static class JsonString extends JsonThing {
		final String value;

		public JsonString(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public String toJson(String indent) {
			return '"' + value + '"';
		}
	}

	// parsing
	// -------
	/**
	 * Parse a JSON string and return the corresponding JsonThing hierarchy.
	 */
	public static JsonThing parse(String json) {
		Cursor cursor = new Cursor(json);
		JsonThing result = parseValue(cursor);
		skipWhitespace(cursor);
		if (cursor.pos != json.length())
			throw new RuntimeException("Unexpected content at position " + cursor.pos);
		return result;
	}

	/** Tracks the current position in the input string across recursive calls. */
	private static class Cursor {
		final String input;
		int pos;
		Cursor(String input) { this.input = input; }

		char peek() {
			skipWhitespace(this);
			if (pos >= input.length())
				throw new RuntimeException("Unexpected end of input");
			return input.charAt(pos);
		}

		char consume() {
			char c = peek();
			pos++;
			return c;
		}

		void expect(char c) {
			char actual = consume();
			if (actual != c)
				throw new RuntimeException("Expected '" + c + "' but got '" + actual + "' at position " + (pos - 1));
		}
	}

	private static void skipWhitespace(Cursor cursor) {
		while (cursor.pos < cursor.input.length()
				&& Character.isWhitespace(cursor.input.charAt(cursor.pos))) {
			cursor.pos++;
		}
	}

	private static JsonThing parseValue(Cursor cursor) {
		char c = cursor.peek();
		if (c == '{') return parseObject(cursor);
		if (c == '[') return parseArray(cursor);
		if (c == '"') return parseString(cursor);
		throw new RuntimeException("Unexpected character '" + c + "' at position " + cursor.pos);
	}

	private static JsonObject parseObject(Cursor cursor) {
		JsonObject obj = new JsonObject();
		cursor.expect('{');
		if (cursor.peek() == '}') { cursor.consume(); return obj; }
		do {
			String key = parseStringValue(cursor);
			cursor.expect(':');
			JsonThing value = parseValue(cursor);
			obj.put(key, value);
		} while (consumeIfMatch(cursor, ','));
		cursor.expect('}');
		return obj;
	}

	private static JsonArray parseArray(Cursor cursor) {
		JsonArray arr = new JsonArray();
		cursor.expect('[');
		if (cursor.peek() == ']') { cursor.consume(); return arr; }
		do {
			arr.add(parseValue(cursor));
		} while (consumeIfMatch(cursor, ','));
		cursor.expect(']');
		return arr;
	}

	private static JsonString parseString(Cursor cursor) {
		return new JsonString(parseStringValue(cursor));
	}

	/** Parses a quoted JSON string and returns the unescaped Java string value. */
	private static String parseStringValue(Cursor cursor) {
		cursor.expect('"');
		StringBuilder sb = new StringBuilder();
		while (true) {
			if (cursor.pos >= cursor.input.length())
				throw new RuntimeException("Unterminated string");
			char c = cursor.input.charAt(cursor.pos++);
			if (c == '"') break;
//			if (c != '\\') {
				sb.append(c);
				continue;
//			}
//
//			// Escape sequence
//			char esc = cursor.input.charAt(cursor.pos++);
//			switch (esc) {
//				case '"':  sb.append('"');  break;
//				case '\\': sb.append('\\'); break;
//				case '/':  sb.append('/');  break;
//				case 'b':  sb.append('\b'); break;
//				case 'f':  sb.append('\f'); break;
//				case 'n':  sb.append('\n'); break;
//				case 'r':  sb.append('\r'); break;
//				case 't':  sb.append('\t'); break;
//				case 'u':
//					String hex = cursor.input.substring(cursor.pos, cursor.pos + 4);
//					sb.append((char) Integer.parseInt(hex, 16));
//					cursor.pos += 4;
//					break;
//				default:
//					throw new RuntimeException("Invalid escape '\\" + esc + "'");
//			}
		}
		return sb.toString();
	}

	/** Skips whitespace, then consumes the next character if it matches. */
	private static boolean consumeIfMatch(Cursor cursor, char c) {
		skipWhitespace(cursor);
		if (cursor.pos < cursor.input.length() && cursor.input.charAt(cursor.pos) == c) {
			cursor.pos++;
			return true;
		}
		return false;
	}

	public static String removeTrailingComma(String input) {
		if(input.trim().endsWith(",")) {
			int commaIdx = input.lastIndexOf(',');
			return input.substring(0, commaIdx);
		}
		return input;
	}

	public static void removeTrailingComma(StringBuilder input) {
		// Find the last non-whitespace character
		int end = input.length();
		while (end > 0 && Character.isWhitespace(input.charAt(end - 1)))
			end--;

		// If that character is a comma, remove it too
		if (end > 0 && input.charAt(end - 1) == ',')
			end--;

		input.delete(end, input.length());
	}

	/**
	 * Escapes a plain Java string for safe inclusion as a JSON string value.
	 */
	public static String escape(String input) {
		if (input == null) return "null";

		StringBuilder sb = new StringBuilder(input.length() + 16);
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			switch (c) {
				case '"':  sb.append("\\\""); break;
				case '\\': sb.append("\\\\"); break;
				case '\b': sb.append("\\b");  break;
				case '\f': sb.append("\\f");  break;
				case '\n': sb.append("\\n");  break;
				case '\r': sb.append("\\r");  break;
				case '\t': sb.append("\\t");  break;
				default:
					if (c < 0x20) {
						sb.append(String.format("\\u%04x", (int) c));
					} else {
						sb.append(c);
					}
			}
		}
		return sb.toString();
	}

	public static String removeControlCharacters(String input) {
		if(input == null)
			return "null";

		StringBuilder sb = new StringBuilder(input.length() + 16);
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if(c < 32 // non-printable ascii characters, includes \b, \f, \n, \r, \t
					|| c == '"' || c == '\\' || c == '/') {
				; // skip those
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Unescapes a JSON string value back to a plain Java string.
	 * Pass the content between the surrounding quotes, not the quotes themselves.
	 */
	public static String unescape(String input) {
		if (input == null) return null;

		StringBuilder sb = new StringBuilder(input.length());
		int i = 0;
		while (i < input.length()) {
			char c = input.charAt(i);
			if (c != '\\') {
				sb.append(c);
				i++;
				continue;
			}

			if (i + 1 >= input.length())
				throw new IllegalArgumentException("Trailing backslash at index " + i);

			char next = input.charAt(i + 1);
			switch (next) {
				case '"':  sb.append('"');  i += 2; break;
				case '\\': sb.append('\\'); i += 2; break;
				case '/':  sb.append('/');  i += 2; break;
				case 'b':  sb.append('\b'); i += 2; break;
				case 'f':  sb.append('\f'); i += 2; break;
				case 'n':  sb.append('\n'); i += 2; break;
				case 'r':  sb.append('\r'); i += 2; break;
				case 't':  sb.append('\t'); i += 2; break;
				case 'u':
					if (i + 5 >= input.length())
						throw new IllegalArgumentException("Incomplete \\uXXXX at index " + i);
					sb.append((char) Integer.parseInt(input.substring(i + 2, i + 6), 16));
					i += 6;
					break;
				default:
					throw new IllegalArgumentException("Invalid escape '\\" + next + "' at index " + i);
			}
		}
		return sb.toString();
	}

	public static void main(String[] args) {
		String original  = "Hello \"World\"!\nTab:\there. Control:\u0001 Slash: \\";
		String escaped   = escape(original);
		String roundtrip = unescape(escaped);

		System.out.println("Original : " + original);
		System.out.println("Escaped  : " + escaped);
		System.out.println("Roundtrip: " + roundtrip);
		System.out.println("Match    : " + original.equals(roundtrip));

		String pattern = "Search images" +
				"{\n  }for {pattern:search-pattern} in {field:search-field}" +
				"{\n  }in {group:search-group}" +
				"{\n  }imported {time:import-interval}" +
				"{action:image-action:+}.";
		System.out.println(removeControlCharacters(pattern));
	}
}