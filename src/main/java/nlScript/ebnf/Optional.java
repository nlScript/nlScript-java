package nlScript.ebnf;

import nlScript.Evaluator;
import nlScript.ParsedNode;
import nlScript.core.BNF;
import nlScript.core.Named;
import nlScript.core.NonTerminal;
import nlScript.core.Production;
import nlScript.util.RandomInt;

public class Optional extends Rule {
	public Optional(NonTerminal tgt, Named<?> child) {
		super("optional", tgt, child);
		setEvaluator(Evaluator.ALL_CHILDREN_EVALUATOR);
	}

	public Named<?> getEntry() {
		return children[0];
	}

	@Override
	public void createBNF(BNF g) {
		final Production p1 = addProduction(g, this, tgt, children[0].getSymbol());
		final Production p2 = addProduction(g, this, tgt);

		p1.onExtension((parent, children) -> {
			ParsedNode c0 = (ParsedNode) children[0];
			c0.setNthEntryInParent(0);
			c0.setName(getParsedNameForChild(0));
		});

		p1.setAstBuilder(Production.AstBuilder.DEFAULT);
	}
}
