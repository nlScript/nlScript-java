package de.nls.core;

public abstract class Symbol {

	private final String symbol;

	public Symbol(String symbol) {
		this.symbol = symbol;
	}

	public String getSymbol() {
		return symbol;
	}

	public boolean isTerminal() {
		return (this instanceof Terminal);
	}

	public boolean isNonTerminal() {
		return (this instanceof NonTerminal);
	}

	public boolean isEpsilon() {
		return (this instanceof Terminal.Epsilon);
	}

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
