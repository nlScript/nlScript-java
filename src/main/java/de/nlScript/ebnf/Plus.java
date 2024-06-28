package de.nlScript.ebnf;

import de.nlScript.Evaluator;
import de.nlScript.ParsedNode;
import de.nlScript.core.BNF;
import de.nlScript.core.NonTerminal;
import de.nlScript.core.Production;
import de.nlScript.core.Symbol;

public class Plus extends Rule {

	public Plus(NonTerminal tgt, Symbol child) {
		super("plus", tgt, child);
		setEvaluator(Evaluator.ALL_CHILDREN_EVALUATOR);
	}

	public Symbol getEntry() {
		return children[0];
	}

	public void createBNF(BNF grammar) {
		Production p1 = addProduction(grammar, this, tgt, children[0], tgt);
		Production p2 = addProduction(grammar, this, tgt, children[0]);

		p1.onExtension((parent, children) -> {
			int nthEntry = ((ParsedNode)parent).getNthEntryInParent();
			ParsedNode c0 = (ParsedNode) children[0];
			ParsedNode c1 = (ParsedNode) children[1];

			c0.setNthEntryInParent(nthEntry);
			c0.setName(getNameForChild(nthEntry));
			c1.setNthEntryInParent(nthEntry + 1);
			c1.setName(parent.getName());
		});

		p2.onExtension((parent, children) -> {
			int nthEntry = ((ParsedNode)parent).getNthEntryInParent();
			ParsedNode c0 = (ParsedNode) children[0];
			c0.setNthEntryInParent(nthEntry);
			c0.setName(getNameForChild(nthEntry));
		});

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
