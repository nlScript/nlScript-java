package nlScript.ebnf;

import nlScript.core.NonTerminal;
import nlScript.core.Production;
import nlScript.core.Symbol;

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
