package nlScript.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import nlScript.ebnf.EBNFCore;
import nlScript.util.RandomInt;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Random;

public interface Generator {

	float DEFAULT_FLOAT_MIN        = -1000.0f;
	float DEFAULT_FLOAT_MAX        = 1000.0f;
	int   DEFAULT_FLOAT_N_DECIMALS = 2;
	int   DEFAULT_INT_MIN          = -1000;
	int   DEFAULT_INT_MAX          = 1000;

	Generation generate(EBNFCore grammar, GeneratorHints hints);

	default Generator fromChild(String child) {
		final Generator myself = this;
		return  (grammar, hints) -> myself.generate(grammar, hints).getChild(child);
	}

	static Generator doubleNumber(double min, double max, int decimals) {
		return (grammar, hints) -> {
			double f = min + (max - min) * (float) Math.random();
			String fStr = decimals == -1 ? Double.toString(f) : format(f, decimals);
			return new Generation(fStr);
		};
	}

	static Generator intNumber(int min, int max) {
		return (grammar, hints) ->
				new Generation(Integer.toString(RandomInt.next(min, max)));
	}

	static Generator string(String... patternsWithRange) {
		return (grammar, hints) -> {
			String s = "";
			for(String patternWithRange : patternsWithRange)
				s += randomString(patternWithRange);
			return new Generation(s);
		};
	}

	static String randomString(String pattern, int minLength, int maxLength) {
		Terminal.CharacterClass c = (Terminal.CharacterClass) Terminal.characterClass(pattern);
		int len = RandomInt.next(minLength, maxLength);
		char[] s = new char[len];
		for (int i = 0; i < len; i++)
			s[i] = c.generate().getGeneratedText().charAt(0);
		return new String(s);
	}

	static String randomLiteral(String literal, int minRepeats, int maxRepeats) {
		int len = RandomInt.next(minRepeats, maxRepeats);
		String ret = "";
		for(int i = 0; i < len; i++)
			ret += literal;
		return ret;
	}

	static String randomStringOrLiteral(String patternOrLiteral, boolean isLiteral, int min, int max) {
		return isLiteral ? randomLiteral(patternOrLiteral, min, max) : randomString(patternOrLiteral, min, max);
	}

	static String randomString(String patternWithRangeOrLiteral) {
		// "[a-z0-9]:0-3"
		// "^[a-z]:*"
		// "[a-z]:?"
		// "[a-z]:+"
		// "[a-z]:5"
		// "[a-z]"
		// "lkjl"
		// "lkjl:2"
		// ...
		// idea
		// check if it's a pattern or a literal:
		// if it starts either with '[' or '^[' and it ends with ']' or contains ']:' and the remainder is a valid range,
		//
 		// another idea: check if there is a range (parse from last ':'). If there is, take everything before that
		// as either pattern or range, depending whether it starts with '^[' and ends with ']' or not.

		int[] range = null;
		int lastIndexOfColon = patternWithRangeOrLiteral.lastIndexOf(":");
		if(lastIndexOfColon >= 0) {
			int remainderStart = lastIndexOfColon + 1;
			if (remainderStart < patternWithRangeOrLiteral.length()) {
				try {
					range = parseRange(patternWithRangeOrLiteral.substring(remainderStart));
				} catch (IllegalArgumentException e) {
					// leave range null
				}
			}
		}

		String patternOrLiteral = range == null
				? patternWithRangeOrLiteral
				: patternWithRangeOrLiteral.substring(0, lastIndexOfColon);

		boolean isPattern = (patternOrLiteral.startsWith("[") || patternOrLiteral.startsWith("^[")) &&
				(patternOrLiteral.endsWith("]"));

		if(range == null)
			range = new int[] { 1, 1 };

		return randomStringOrLiteral(patternOrLiteral, !isPattern, range[0], range[1]);
	}

	static int[] parseRange(String s) {
		s = s.trim();
		if(s.contains("-")) {
			String[] toks = s.split("-");
			try {
				int from = Integer.parseInt(toks[0]);
				int to = Integer.parseInt(toks[1]);
				return new int[] { from, to };
			} catch(NumberFormatException e) {
				throw new IllegalArgumentException();
			}
		}

		if(s.equals("*"))
			return new int[] { 0, Integer.MAX_VALUE };

		if(s.equals("+"))
			return new int[] { 1, Integer.MAX_VALUE };

		if(s.equals("?"))
			return new int[] { 0, 1 };

		try {
			int fromto = Integer.parseInt(s);
			return new int[]{fromto, fromto};
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException();
		}
	}

	enum Order {
		ORIGINAL_ORDER,
		RANDOM,
		ALPHABETICAL
	}

	static <T> Generator fromList(List<T> items, int minNo, int maxNo, Order order, boolean withRepeats) {
		return (grammar, hints) -> {
			if (items == null || items.isEmpty())
				throw new IllegalArgumentException("items must not be null or empty");

			int min = minNo;
			int max = maxNo;
			if(min < 0)
				min = 0;
			if(max < 0)
				max = items.size();

			int n = RandomInt.next(min, max);
			if (n < 0)
				throw new IllegalArgumentException("n must not be negative");

			if (!withRepeats && n > items.size()) {
				throw new IllegalArgumentException(
						"n (" + n + ") cannot exceed the number of available items (" + items.size() +
								") when withRepeats is false");
			}

			// Collect the indices of the drawn items first — index-based
			// throughout means no dependence on equals()/hashCode(), and
			// duplicates in `items` are handled correctly automatically.
			List<Integer> drawnIndices = new ArrayList<>(n);
			Random RNG = new Random();

			if (withRepeats) {
				for (int i = 0; i < n; i++)
					drawnIndices.add(RNG.nextInt(items.size()));
			} else {
				List<Integer> allIndices = new ArrayList<>(items.size());
				for (int i = 0; i < items.size(); i++)
					allIndices.add(i);
				Collections.shuffle(allIndices, RNG);
				drawnIndices.addAll(allIndices.subList(0, n));
			}

			// Arrange according to the requested order, while we still have indices.
			switch (order) {
				case ORIGINAL_ORDER:
					Collections.sort(drawnIndices); // stable, keeps duplicate indices adjacent
					break;
				case RANDOM:
					Collections.shuffle(drawnIndices, RNG);
					break;
				case ALPHABETICAL:
					drawnIndices.sort(Comparator.comparing(i -> items.get(i).toString()));
					break;
				default:
					throw new IllegalStateException("Unknown order: " + order);
			}

			// Translate indices to actual items only at the very end.
			List<String> result = new ArrayList<>(n);
			for (int idx : drawnIndices) {
				result.add(items.get(idx).toString());
			}
			return new Generation(String.join(", ", result));
		};
	}

	static String format(double f, int decimalDigits) {
		StringBuilder sb = new StringBuilder("#");
		if(decimalDigits > 0)
			sb.append('.');
		for(int i = 0; i < decimalDigits; i++)
			sb.append('#');

		DecimalFormat df = new DecimalFormat(sb.toString());
		DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		dfs.setDecimalSeparator('.');
		df.setDecimalFormatSymbols(dfs);
		df.setGroupingUsed(false);
		return df.format(f);
	}
}
