package de.nls.ebnf;

import de.nls.core.BNF;
import de.nls.core.NonTerminal;
import de.nls.core.Symbol;

class Plus extends Rule {
	public Plus(NonTerminal tgt, Symbol child) {
		super("plus", tgt, child);
	}

	public void createBNF(BNF grammar) {
		addProduction(grammar, this, tgt, children[0], tgt);
		addProduction(grammar, this, tgt, children[0]);
	}
}
