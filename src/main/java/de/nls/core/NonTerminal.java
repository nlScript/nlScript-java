package de.nls.core;

import de.nls.util.RandomString;

public class NonTerminal extends Symbol {

	private static final RandomString rs = new RandomString(8);

	public NonTerminal(String symbol) {
		super(symbol != null ? symbol : makeRandomSymbol());
	}

	@Override
	public String toString() {
		return "<" + getSymbol() + ">";
	}

	public static String makeRandomSymbol() {
		return rs.nextString();
	}
}