package de.nls.core;

import java.util.ArrayList;
import java.util.HashMap;

public class BNF {

	public static final NonTerminal ARTIFICIAL_START_SYMBOL = new NonTerminal("S'");
	public static final Terminal    ARTIFICIAL_STOP_SYMBOL  = new Terminal.EndOfInput();
	public static final Terminal    EPSILON                 = new Terminal.Epsilon();
	public static final Terminal    DIGIT                   = new Terminal.Digit();
	public static final Terminal    LETTER                  = new Terminal.Letter();

	private final HashMap<String, Symbol> symbols = new HashMap<>();

	private final ArrayList<Production> productions = new ArrayList<>();

	public Production addProduction(Production p) {
		if(productions.contains(p))
			return productions.get(productions.indexOf(p));
		productions.add(p);
		symbols.put(p.getLeft().getSymbol(), p.getLeft());
		for(Symbol s : p.getRight()) {
			if(!s.isEpsilon())
				symbols.put(s.getSymbol(), s);
		}
		return p;
	}

	public static Terminal literal(String s) {
		return new Terminal.Literal(s);
	}

	public Symbol getSymbol(String symbol) {
		Symbol ret = symbols.get(symbol);
		if(ret == null)
			throw new RuntimeException("Could not find symbol " + symbol);
		return ret;
	}

	public ArrayList<Production> getProductions(NonTerminal left) {
		ArrayList<Production> ret = new ArrayList<>(productions);
		ret.removeIf(p -> !p.getLeft().equals(left));
		return ret;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(Production p : productions)
			sb.append(p.toString()).append(System.lineSeparator());
		return sb.toString();
	}
}
