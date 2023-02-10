package de.nls.ebnf;

import de.nls.core.BNF;
import de.nls.core.DefaultParsedNode;
import de.nls.core.NonTerminal;
import de.nls.ParsedNode;
import de.nls.core.Production;
import de.nls.core.Symbol;

class Sequence extends Rule {
	public Sequence(NonTerminal tgt, Symbol... children) {
		super("sequence", tgt, children);
		// don't set an evaluator for sequences... setEvaluator(allChildEvaluator);
	}

	public void createBNF(BNF g) {
		Production p = addProduction(g, this, tgt, children);
		p.onExtension((parent, children) -> {
			for(int c = 0; c < children.length; c++) {
				ParsedNode ch = (ParsedNode) children[c];
				ch.setNthEntryInParent(c);
				ch.setName(getNameForChild(c));
			}
		});
		p.setAstBuilder((DefaultParsedNode::addChildren));
	}
}
