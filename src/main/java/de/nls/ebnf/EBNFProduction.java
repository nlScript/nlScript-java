package de.nls.ebnf;

import de.nls.core.NonTerminal;
import de.nls.core.Production;
import de.nls.core.Symbol;

public class EBNFProduction extends Production {

	private final Rule rule;

	public EBNFProduction(Rule rule, NonTerminal left, Symbol... right) {
		super(left, right);
		this.rule = rule;
	}

	public Rule getRule() {
		return rule;
	}
}
