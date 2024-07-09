package nlScript.core;

import java.util.ArrayList;
import java.util.HashSet;

public abstract class Terminal extends Symbol {

	public static final Terminal EPSILON      = new Epsilon();
	public static final Terminal DIGIT        = new Digit();
	public static final Terminal LETTER       = new Letter();
	public static final Terminal WHITESPACE   = new Whitespace();

	public static final Terminal END_OF_INPUT = new EndOfInput();

	public static Terminal literal(String s) {
		return new Literal(s);
	}

	public static Terminal characterClass(String pattern) {
		return new CharacterClass(pattern);
	}

	public Terminal(String symbol) {
		super(symbol);
	}

	@Override
	public boolean isTerminal() {
		return true;
	}

	@Override
	public boolean isNonTerminal() {
		return false;
	}

	@Override
	public boolean isEpsilon() {
		return false;
	}

	public abstract Matcher matches(Lexer lexer);

	public abstract Object evaluate(Matcher matcher);

	public Named<Terminal> withName(String name) {
		return new Named<>(this, name);
	}

	public Named<Terminal> withName() {
		return new Named<>(this);
	}

	public static class Epsilon extends Terminal {
		private Epsilon() {
			super("epsilon");
		}

		@Override
		public boolean isEpsilon() {
			return true;
		}

		@Override
		public Matcher matches(Lexer lexer) {
			return new Matcher(ParsingState.SUCCESSFUL, lexer.getPosition(), "");
		}

		@Override
		public Object evaluate(Matcher matcher) {
			return null;
		}
	}

	public static class EndOfInput extends Terminal {
		private EndOfInput() {
			super("EOI");
		}

		@Override
		public Matcher matches(Lexer lexer) {
			int pos = lexer.getPosition();
			if(lexer.isAtEnd())
				return new Matcher(ParsingState.SUCCESSFUL, pos, " ");
			return new Matcher(ParsingState.FAILED, pos, "");
		}

		@Override
		public Object evaluate(Matcher matcher) {
			return null;
		}
	}

	public static class Digit extends Terminal {
		private Digit() {
			super("digit");
		}

		@Override
		public Matcher matches(Lexer lexer) {
			int pos = lexer.getPosition();
			if(lexer.isAtEnd())
				return new Matcher(ParsingState.END_OF_INPUT, pos, "");
			char c = lexer.peek();
			if(Character.isDigit(c))
				return new Matcher(ParsingState.SUCCESSFUL, pos, Character.toString(c));
			return new Matcher(ParsingState.FAILED, pos, Character.toString(c));
		}

		@Override
		public Object evaluate(Matcher matcher) {
			return matcher.parsed.charAt(0);
		}
	}

	public static class Literal extends Terminal {
		private Literal(String symbol) {
			super(symbol);
		}

		@Override
		public Matcher matches(Lexer lexer) {
			int pos = lexer.getPosition();
			String symbol = getSymbol();
			for(int i = 0; i < symbol.length(); i++) {
				if(lexer.isAtEnd(i))
					return new Matcher(ParsingState.END_OF_INPUT, pos, lexer.substring(pos, pos + i + 1));
				if(lexer.peek(i) != symbol.charAt(i))
					return new Matcher(ParsingState.FAILED, pos, lexer.substring(pos, pos + i + 1));
			}
			return new Matcher(ParsingState.SUCCESSFUL, pos, symbol);
		}

		@Override
		public Object evaluate(Matcher matcher) {
			return matcher.parsed;
		}

		@Override
		public String toString() {
			return "'" + getSymbol() + "'";
		}
	}

	public static class Letter extends Terminal {
		private Letter() {
			super("letter");
		}

		@Override
		public Matcher matches(Lexer lexer) {
			int pos = lexer.getPosition();
			if(lexer.isAtEnd())
				return new Matcher(ParsingState.END_OF_INPUT, pos, "");
			char c = lexer.peek();
			if(Character.isLetter(c))
				return new Matcher(ParsingState.SUCCESSFUL, pos, Character.toString(c));
			return new Matcher(ParsingState.FAILED, pos, Character.toString(c));
		}

		@Override
		public Object evaluate(Matcher matcher) {
			return matcher.parsed.charAt(0);
		}
	}

	public static class Whitespace extends Terminal {
		private Whitespace() {
			super("whitespace");
		}

		@Override
		public Matcher matches(Lexer lexer) {
			int pos = lexer.getPosition();
			if(lexer.isAtEnd())
				return new Matcher(ParsingState.END_OF_INPUT, pos, "");
			char c = lexer.peek();
			if(c == ' ' || c == '\t')
				return new Matcher(ParsingState.SUCCESSFUL, pos, Character.toString(c));
			return new Matcher(ParsingState.FAILED, pos, Character.toString(c));
		}

		@Override
		public Object evaluate(Matcher matcher) {
			return matcher.parsed.charAt(0);
		}
	}

	public static class CharacterClass extends Terminal {

		private final Ranges ranges;

		private CharacterClass(String pattern) {
			super(pattern);
			StringBuilder b = new StringBuilder(pattern.trim());
			if(b.length() == 0)
				throw new RuntimeException("empty character class pattern");
			if(b.charAt(0) != '[' || b.charAt(b.length() - 1) != ']')
				throw new RuntimeException("Wrong character class format: " + pattern);

			int start = 1;
			int end = b.length() - 2;

			boolean negated = b.charAt(1) == '^';
			if(negated)
				start++;

			ranges = new Ranges(negated);

			if(b.charAt(start) == '-') {
				ranges.add(new SingleCharacterRange('-'));
				start++;
			}
			if(b.charAt(end) == '-') {
				ranges.add(new SingleCharacterRange('-'));
				end--;
			}

			int idx = start;
			while(idx <= end) {
				int nIdx = idx + 1;
				char c = b.charAt(idx);
				if(nIdx <= end && b.charAt(nIdx) == '-') {
					char u = b.charAt(idx + 2);
					if(c == '-' || u == '-')
						throw new RuntimeException("Wrong character class format: " + pattern);
					ranges.add(new CharacterRange(c, u));
					idx = idx + 3;
				}
				else {
					ranges.add(new SingleCharacterRange(c));
					idx++;
				}
			}
		}

		@Override
		public Matcher matches(Lexer lexer) {
			int pos = lexer.getPosition();
			if(lexer.isAtEnd())
				return new Matcher(ParsingState.END_OF_INPUT, pos, "");
			char c = lexer.peek();
			if(ranges.checkCharacter(c))
				return new Matcher(ParsingState.SUCCESSFUL, pos, Character.toString(c));
			return new Matcher(ParsingState.FAILED, pos, Character.toString(c));
		}

		@Override
		public Object evaluate(Matcher matcher) {
			return matcher.parsed.charAt(0);
		}

		@Override
		public String toString() {
			String ret = super.toString();
			ret = ret.replaceAll("\n", "\\n");
			return ret;
		}
	}

	private static class CharacterRange {
		int lower;
		int upper;

		public CharacterRange(int lower, int upper) {
			this.lower = lower;
			this.upper = upper;
		}

		public boolean checkCharacter(int i) {
			return i >= lower && i <= upper;
		}

		public boolean equals(Object o) {
			if(!o.getClass().equals(getClass()))
				return false;
			CharacterRange c = (CharacterRange) o;
			return lower == c.lower && upper == c.upper;
		}
	}

	private static class SingleCharacterRange extends CharacterRange {
		int number;

		public SingleCharacterRange(int number) {
			super(0, 0);
			this.number = number;
		}

		@Override
		public boolean checkCharacter(int i) {
			return i == number;
		}
	}

	private static class Ranges {

		private final ArrayList<CharacterRange> ranges = new ArrayList<>();

		private final boolean negated;

		public Ranges(boolean negated) {
			this.negated = negated;
		}

		public void add(CharacterRange range) {
			ranges.add(range);
		}

		public boolean checkCharacter(int i) {
			for(CharacterRange range : ranges) {
				boolean check = range.checkCharacter(i);
				if(!negated && check)
					return true;
				if(negated && check)
					return false;
			}
			return negated;
		}

		public boolean equals(Object o) {
			if(!o.getClass().equals(this.getClass()))
				return false;
			HashSet<CharacterRange> ts = new HashSet<>(ranges);
			HashSet<CharacterRange> os = new HashSet<>(((Ranges)o).ranges);
			return ts.equals(os);
		}
	}
}
