package de.nls.core;

public abstract class Symbol implements RepresentsSymbol {

	private final String symbol;

	public Symbol(String symbol) {
		this.symbol = symbol;
	}

	public String getSymbol() {
		return symbol;
	}

	public Symbol getRepresentedSymbol() {
		return this;
	}

	public abstract boolean isTerminal();

	public abstract boolean isNonTerminal();

	public abstract boolean isEpsilon();

	@Override
	public String toString() {
		return symbol;
	}

	@Override
	public boolean equals(Object o) {
		if(o.getClass() != getClass())
			return false;
		return symbol.equals(((Symbol) o).symbol);
	}

	@Override
	public int hashCode() {
		return symbol.hashCode();
	}
}
