package de.nls.ebnf;

import de.nls.Evaluator;
import de.nls.core.BNF;
import de.nls.core.DefaultParsedNode;
import de.nls.core.NonTerminal;
import de.nls.ParsedNode;
import de.nls.core.Production;
import de.nls.core.Symbol;

public class Optional extends Rule {
	public Optional(NonTerminal tgt, Symbol child) {
		super("optional", tgt, child);
		setEvaluator(Evaluator.ALL_CHILDREN_EVALUATOR);
	}

	public Symbol getEntry() {
		return children[0];
	}

	@Override
	public void createBNF(BNF g) {
		final Production p1 = addProduction(g, this, tgt, children[0]);
		final Production p2 = addProduction(g, this, tgt);

		p1.onExtension((parent, children) -> {
			ParsedNode c0 = (ParsedNode) children[0];
			c0.setNthEntryInParent(0);
			c0.setName(getNameForChild(0));
		});

		p1.setAstBuilder(DefaultParsedNode::addChildren);
	}
}
