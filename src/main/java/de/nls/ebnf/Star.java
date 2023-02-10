package de.nls.ebnf;

import de.nls.Evaluator;
import de.nls.ParsedNode;
import de.nls.core.BNF;
import de.nls.core.NonTerminal;
import de.nls.core.Production;
import de.nls.core.Symbol;

public class Star extends Rule {
	public Star(NonTerminal tgt, Symbol child) {
		super("star", tgt, child);
		setEvaluator(Evaluator.ALL_CHILDREN_EVALUATOR);
	}

	public Symbol getEntry() {
		return children[0];
	}

	public void createBNF(BNF grammar) {
		final Production p1 = addProduction(grammar, this, tgt, children[0], tgt);
		final Production p2 = addProduction(grammar, this, tgt);

		p1.onExtension((parent, children) -> {
			int nthEntry = ((ParsedNode)parent).getNthEntryInParent();
			ParsedNode c0 = (ParsedNode) children[0];
			ParsedNode c1 = (ParsedNode) children[1];

			c0.setNthEntryInParent(nthEntry);
			c0.setName(getNameForChild(nthEntry));
			c1.setNthEntryInParent(nthEntry + 1);
			c1.setName(parent.getName());
		});

		p1.setAstBuilder((parent, children) -> {
			// collect the ParsedNode from the first child and add all children of the 2nd child
			parent.addChildren(children[0]);
			parent.addChildren(children[1].getChildren());
		});
	}
}