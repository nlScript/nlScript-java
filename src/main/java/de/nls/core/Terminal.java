package de.nls.core;

public class Terminal extends Symbol {

	public Terminal(String symbol) {
		super(symbol);
	}

	@Override
	public String toString() {
		return getSymbol();
	}

	public static class Epsilon extends Terminal {
		Epsilon() {
			super("epsilon");
		}
	}

	public static class EndOfInput extends Terminal {
		EndOfInput() {
			super("EOI");
		}
	}
}
