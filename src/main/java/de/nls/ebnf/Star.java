package de.nls.ebnf;

import de.nls.Evaluator;
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
			int nthEntry = parent.getNthEntryInParent();
			children[0].setNthEntryInParent(nthEntry);
			children[0].setName(getNameForChild(nthEntry));
			children[1].setNthEntryInParent(nthEntry + 1);
			children[1].setName(parent.getName());
		});

		p1.setAstBuilder((parent, children) -> {
			// collect the ParsedNode from the first child and add all children of the 2nd child
			parent.addChildren(children[0]);
			parent.addChildren(children[1].getChildren());
		});
	}
}