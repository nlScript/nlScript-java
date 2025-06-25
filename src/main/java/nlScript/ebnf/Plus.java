package nlScript.ebnf;

import nlScript.Evaluator;
import nlScript.ParsedNode;
import nlScript.core.BNF;
import nlScript.core.Named;
import nlScript.core.NonTerminal;
import nlScript.core.Production;
import nlScript.util.RandomInt;

import java.util.Random;

public class Plus extends Rule {

	public Plus(NonTerminal tgt, Named<?> child) {
		super("plus", tgt, child);
		setEvaluator(Evaluator.ALL_CHILDREN_EVALUATOR);
	}

	public Named<?> getEntry() {
		return children[0];
	}

	public void createBNF(BNF grammar) {
		Production p1 = addProduction(grammar, this, tgt, children[0].getSymbol(), tgt);
		Production p2 = addProduction(grammar, this, tgt, children[0].getSymbol());

		p1.onExtension((parent, children) -> {
			int nthEntry = ((ParsedNode)parent).getNthEntryInParent();
			ParsedNode c0 = (ParsedNode) children[0];
			ParsedNode c1 = (ParsedNode) children[1];

			c0.setNthEntryInParent(nthEntry);
			c0.setName(getParsedNameForChild(nthEntry));
			c1.setNthEntryInParent(nthEntry + 1);
			c1.setName(parent.getName());
		});

		p2.onExtension((parent, children) -> {
			int nthEntry = ((ParsedNode)parent).getNthEntryInParent();
			ParsedNode c0 = (ParsedNode) children[0];
			c0.setNthEntryInParent(nthEntry);
			c0.setName(getParsedNameForChild(nthEntry));
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
