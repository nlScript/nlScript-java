package nlScript.core;

import nlScript.util.RandomString;

import java.util.ArrayList;
import java.util.HashSet;

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

	/**
	 * Checks recursively if this <code>NonTerminal</code> uses the specified symbol in any sub-production
	 * @param symbol <code>Symbol</code>to check
	 * @return whether this <code>NonTerminal</code> uses symbol
	 */
	public boolean uses(Symbol symbol, BNF bnf) {
		return uses(symbol, bnf, new HashSet<>());
	}

	private boolean uses(Symbol symbol, BNF bnf, HashSet<Production> progressing) {
		ArrayList<Production> productions = bnf.getProductions(this);
		for(Production p : productions) {
			if(progressing.contains(p))
				continue;
			progressing.add(p);
			Symbol[] rhs = p.getRight();
			for(Symbol rSym : rhs) {
				if(rSym.equals(symbol))
					return true;
				else if(rSym instanceof NonTerminal) {
					if(((NonTerminal) rSym).uses(symbol, bnf,progressing))
						return true;
				}
			}
		}
		return false;
	}

	@Override
	public String toString() {
		return "<" + getSymbol() + ">";
	}

	public static String makeRandomSymbol() {
		return rs.nextString();
	}
}
