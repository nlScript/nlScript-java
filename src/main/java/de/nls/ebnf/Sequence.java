package de.nls.ebnf;

import de.nls.core.BNF;
import de.nls.core.NonTerminal;
import de.nls.core.Symbol;

class Sequence extends Rule {
	public Sequence(NonTerminal tgt, Symbol... children) {
		super("sequence", tgt, children);
		// don't set an evaluator for sequences... setEvaluator(allChildEvaluator);
	}

	public void createBNF(BNF g) {
		addProduction(g, this, tgt, children);
	}
}
