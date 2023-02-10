package de.nls.ebnf;

import de.nls.Evaluator;
import de.nls.core.BNF;
import de.nls.core.DefaultParsedNode;
import de.nls.core.NonTerminal;
import de.nls.ParsedNode;
import de.nls.core.Production;
import de.nls.core.Symbol;

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
			p.setAstBuilder(DefaultParsedNode::addChildren);
		}
	}
}
