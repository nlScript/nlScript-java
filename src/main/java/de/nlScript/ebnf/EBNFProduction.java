package de.nlScript.ebnf;

import de.nlScript.core.NonTerminal;
import de.nlScript.core.Production;
import de.nlScript.core.Symbol;

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
