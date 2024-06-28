package de.nlScript.ebnf;

import de.nlScript.Evaluator;
import de.nlScript.core.BNF;
import de.nlScript.core.NonTerminal;
import de.nlScript.ParsedNode;
import de.nlScript.core.Production;
import de.nlScript.core.Symbol;

public class Or extends Rule {
	public Or(NonTerminal tgt, Symbol... children) {
		super("or", tgt, children);
		setEvaluator(Evaluator.FIRST_CHILD_EVALUATOR);
	}

	public void createBNF(BNF grammar) {
		for(int io = 0; io < children.length; io++) {
			final int fio = io;
			Symbol option = children[io];
			Production p = addProduction(grammar, this, tgt, option);
			p.onExtension((parent, children) -> {
				ParsedNode c0 = (ParsedNode) children[0];
				c0.setNthEntryInParent(fio);
				c0.setName(getNameForChild(fio));
			});
			p.setAstBuilder(Production.AstBuilder.DEFAULT);
		}
	}
}
