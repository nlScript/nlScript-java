package nlScript.ebnf;

import nlScript.ParsedNode;
import nlScript.core.BNF;
import nlScript.core.NonTerminal;
import nlScript.core.Production;
import nlScript.core.Symbol;

public class Sequence extends Rule {
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
		p.setAstBuilder(Production.AstBuilder.DEFAULT);
	}
}
