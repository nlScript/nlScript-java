package de.nls.ebnf;

import de.nls.core.BNF;
import de.nls.core.NonTerminal;
import de.nls.core.Symbol;

public abstract class Rule {
	protected final String type;
	protected final NonTerminal tgt;
	protected final Symbol[] children;
	protected String[] parsedChildNames;

	public Rule(String type, NonTerminal tgt, Symbol... children) {
		this.type = type;
		this.tgt = tgt != null
				? tgt
				: new NonTerminal(type + ":" + NonTerminal.makeRandomSymbol());
		this.children = children;
	}

	public NonTerminal getTarget() {
		return tgt;
	}

	public static EBNFProduction addProduction(BNF grammar, Rule rule, NonTerminal left, Symbol... right) {
		EBNFProduction production = new EBNFProduction(rule, left, right);
		grammar.addProduction(production);
		return production;
	}

	protected String getNameForChild(int idx) {
		if(parsedChildNames == null) {
			System.out.println("parsedChildNames not set");
			return null;
		}
		if(parsedChildNames.length == 1)
			return parsedChildNames[0];
		if(idx >= parsedChildNames.length)
			return "no name";
		return parsedChildNames[idx];
	}

	public void setParsedChildNames(String... parsedChildNames) {
		this.parsedChildNames = parsedChildNames;
	}

	public abstract void createBNF(BNF grammar);
}
