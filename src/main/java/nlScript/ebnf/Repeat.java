package nlScript.ebnf;

import nlScript.Evaluator;
import nlScript.ParsedNode;
import nlScript.core.BNF;
import nlScript.core.Named;
import nlScript.core.NonTerminal;
import nlScript.core.Production;
import nlScript.core.Symbol;
import nlScript.util.RandomInt;

public class Repeat extends Rule {

	private final int from;
	private final int to;

	public Repeat(NonTerminal tgt, Named<?> child, int from, int to) {
		super("repeat", tgt, child);
		this.from = from;
		this.to = to;
		setEvaluator(Evaluator.ALL_CHILDREN_EVALUATOR);
	}

	public int getFrom() {
		return from;
	}

	public int getTo() {
		return to;
	}

	public Named<?> getEntry() {
		return children[0];
	}

	public void createBNF(BNF g) {
		for(int seqLen = to; seqLen >= from; seqLen--) {
			Symbol[] rhs = new Symbol[seqLen];
			for(int i = 0; i < seqLen; i++)
				rhs[i] = children[0].getSymbol();
			Production p = addProduction(g, this, tgt, rhs);
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
}