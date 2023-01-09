package de.nls.ebnf;

import de.nls.Evaluator;
import de.nls.core.BNF;
import de.nls.core.NonTerminal;
import de.nls.core.ParsedNode;
import de.nls.core.Production;
import de.nls.core.Symbol;

public class Or extends Rule {
	public Or(NonTerminal tgt, Symbol... children) {
		super("or", tgt, children);
		setEvaluator(Evaluator.FIRST_CHILD_EVALUATOR);
	}

	public void createBNF(BNF grammar) {
		for(Symbol option : children) {
			Production p = addProduction(grammar, this, tgt, option);
			p.setAstBuilder((ParsedNode::addChildren));
		}
	}
}
