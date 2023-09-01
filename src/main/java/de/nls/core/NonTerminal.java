package de.nls.core;

import de.nls.util.RandomString;

public class NonTerminal extends Symbol {

	private static final RandomString rs = new RandomString(8);

	public NonTerminal(String symbol) {
		super(symbol != null ? symbol : makeRandomSymbol());
	}

	@Override
	public boolean isTerminal() {
		return false;
	}

	@Override
	public boolean isNonTerminal() {
		return true;
	}

	@Override
	public boolean isEpsilon() {
		return false;
	}

	public Named<NonTerminal> withName(String name) {
		return new Named<>(this, name);
	}

	public Named<NonTerminal> withName() {
		return new Named<>(this);
	}

	@Override
	public String toString() {
		return "<" + getSymbol() + ">";
	}

	public static String makeRandomSymbol() {
		return rs.nextString();
	}
}