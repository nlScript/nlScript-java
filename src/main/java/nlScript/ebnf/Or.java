package nlScript.ebnf;

import nlScript.Evaluator;
import nlScript.ParsedNode;
import nlScript.core.BNF;
import nlScript.core.Generator;
import nlScript.core.GeneratorHints;
import nlScript.core.Named;
import nlScript.core.NonTerminal;
import nlScript.core.Production;
import nlScript.core.Symbol;

import java.util.Random;

public class Or extends Rule {
	public Or(NonTerminal tgt, Named<?>... children) {
		super("or", tgt, children);
		setEvaluator(Evaluator.FIRST_CHILD_EVALUATOR);
	}

	public void createBNF(BNF grammar) {
		for(int io = 0; io < children.length; io++) {
			final int fio = io;
			Symbol option = children[io].getSymbol();
			Production p = addProduction(grammar, this, tgt, option);
			p.onExtension((parent, children) -> {
				ParsedNode c0 = (ParsedNode) children[0];
				c0.setNthEntryInParent(fio);
				c0.setName(getParsedNameForChild(fio));
			});
			p.setAstBuilder(Production.AstBuilder.DEFAULT);
		}
	}

	private final Generator DEFAULT_GENERATOR = (grammar, hints) -> {
		int n = children.length;
		int r = new Random().nextInt(n);
		String name = getParsedNameForChild(r);
		Generator generator = getChildGenerator(name, grammar, children[r].getSymbol());
		GeneratorHints cHints = getChildGeneratorHints(name);
		return generator.generate(grammar, cHints); // Rule.generate(grammar, children[r]);
	};

	@Override
	public Generator getDefaultGenerator() {
		return DEFAULT_GENERATOR;
	}
}
