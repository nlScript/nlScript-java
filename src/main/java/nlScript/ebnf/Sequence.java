package nlScript.ebnf;

import nlScript.ParsedNode;
import nlScript.core.BNF;
import nlScript.core.Named;
import nlScript.core.NonTerminal;
import nlScript.core.Production;

public class Sequence extends Rule {
	public Sequence(NonTerminal tgt, Named<?>... children) {
		super("sequence", tgt, children);
		// don't set an evaluator for sequences... setEvaluator(allChildEvaluator);
	}

	public void createBNF(BNF g) {
		Production p = addProduction(g, this, tgt, getSymbols(children));
		p.onExtension((parent, children) -> {
			for(int c = 0; c < children.length; c++) {
				ParsedNode ch = (ParsedNode) children[c];
				ch.setNthEntryInParent(c);
				ch.setName(getParsedNameForChild(c));
			}
		});
		p.setAstBuilder(Production.AstBuilder.DEFAULT);
	}
}
