package de.nls.core;

public abstract class Terminal extends Symbol {

	public Terminal(String symbol) {
		super(symbol);
	}

	public abstract Matcher matches(Lexer lexer);

	@Override
	public String toString() {
		return getSymbol();
	}

	public static class Epsilon extends Terminal {
		Epsilon() {
			super("epsilon");
		}

		@Override
		public Matcher matches(Lexer lexer) {
			return new Matcher(ParsingState.SUCCESSFUL, lexer.getPosition(), "");
		}
	}

	public static class EndOfInput extends Terminal {
		EndOfInput() {
			super("EOI");
		}

		@Override
		public Matcher matches(Lexer lexer) {
			int pos = lexer.getPosition();
			if(lexer.isAtEnd())
				return new Matcher(ParsingState.SUCCESSFUL, pos, " ");
			return new Matcher(ParsingState.FAILED, pos, "");
		}
	}

	public static class Digit extends Terminal {
		public Digit() {
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
			return new Matcher(ParsingState.FAILED, pos, "");
		}
	}

	public static class Literal extends Terminal {
		public Literal(String symbol) {
			super(symbol);
		}

		@Override
		public Matcher matches(Lexer lexer) {
			int pos = lexer.getPosition();
			String symbol = getSymbol();
			for(int i = 0; i < symbol.length(); i++) {
				if(lexer.isAtEnd(i))
					return new Matcher(ParsingState.END_OF_INPUT, pos, lexer.substring(pos, pos + i));
				if(lexer.peek(i) != symbol.charAt(i))
					return new Matcher(ParsingState.FAILED, pos, lexer.substring(pos, pos + i));
			}
			return new Matcher(ParsingState.SUCCESSFUL, pos, symbol);
		}

		@Override
		public String toString() {
			return "'" + getSymbol() + "'";
		}
	}
}
