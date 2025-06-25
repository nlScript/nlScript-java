package nlScript.core;

import java.util.HashMap;

public class GeneratorHints {

	public static final String SEP = "::";

	public enum Key {
		MIN_VALUE,
		MAX_VALUE,
		DECIMAL_PLACES,
		MIN_NUMBER,
		MAX_NUMBER
	}

	private HashMap<Key, Object> map;

	public static GeneratorHints from(Object... keysAndValues) {
		int n = keysAndValues.length;
		if(n % 2 != 0)
			throw new RuntimeException("Expected alternately keys and values.");
		GeneratorHints hints = new GeneratorHints();
		for(int i = 0; i < n/2; i++) {
			if(! (keysAndValues[2 * i] instanceof GeneratorHints.Key))
				throw new RuntimeException("Expected alternately keys and values.");
			GeneratorHints.Key key = (GeneratorHints.Key) keysAndValues[i * 2];
			Object value = keysAndValues[i * 2 + 1];
			hints.with(key, value);
		}
		return hints;
	}

	public GeneratorHints with(Key key, Object object) {
		if(map == null)
			map = new HashMap<>();
		map.put(key, object);
		return this;
	}

	public Object get(Key key) {
		if(map == null)
			return null;
		return map.get(key);
	}

	public Object get(Key key, Object defaultValue) {
		Object ret = get(key);
		return ret != null ? ret : defaultValue;
	}
}
