package de.nls.core;

import java.util.ArrayList;
import java.util.HashMap;

public class BNF {

	public static final NonTerminal ARTIFICIAL_START_SYMBOL = new NonTerminal("S'");
	public static final Terminal    ARTIFICIAL_STOP_SYMBOL  = Terminal.END_OF_INPUT;

	private final HashMap<String, Symbol> symbols = new HashMap<>();

	private final ArrayList<Production> productions = new ArrayList<>();

	public BNF() {}

	public BNF(BNF other) {
		this();
		symbols.putAll(other.symbols);
		productions.addAll(other.productions);
	}

	public void reset() {
		symbols.clear();
		productions.clear();
	}

	public void removeStartProduction() {
		productions.removeIf(p -> p.getLeft().equals(ARTIFICIAL_START_SYMBOL));
	}

	public Production addProduction(Production p) {
		if(productions.contains(p)) {
			System.out.println("Production already exists: " + p);
			return productions.get(productions.indexOf(p));
		}
		productions.add(p);
		symbols.put(p.getLeft().getSymbol(), p.getLeft());
		for(Symbol s : p.getRight()) {
			if(!s.isEpsilon())
				symbols.put(s.getSymbol(), s);
		}
		return p;
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
