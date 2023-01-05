package de.nls.ebnf;

import de.nls.Evaluator;
import de.nls.core.BNF;
import de.nls.core.NonTerminal;
import de.nls.core.Production;
import de.nls.core.Symbol;

class Plus extends Rule {

	public Plus(NonTerminal tgt, Symbol child) {
		super("plus", tgt, child);
		setEvaluator(Evaluator.ALL_CHILDREN_EVALUATOR);
	}

	public void createBNF(BNF grammar) {
		Production p1 = addProduction(grammar, this, tgt, children[0], tgt);
		Production p2 = addProduction(grammar, this, tgt, children[0]);

		p1.setAstBuilder((parent, children) -> {
			// collect the ParsedNode from the first child and add all children of the 2nd child
			parent.addChildren(children[0]);
			parent.addChildren(children[1].getChildren());
		});
		//noinspection CodeBlock2Expr
		p2.setAstBuilder((parent, children) -> {
			parent.addChildren(children[0]);
		});
	}
}
