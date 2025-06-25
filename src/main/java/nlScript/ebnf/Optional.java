package nlScript.ebnf;

import nlScript.Evaluator;
import nlScript.ParsedNode;
import nlScript.core.BNF;
import nlScript.core.Generation;
import nlScript.core.Generator;
import nlScript.core.GeneratorHints;
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

	private final Generator DEFAULT_GENERATOR = (grammar, hints) -> {
		int n = RandomInt.next(0, 1);
		StringBuilder generatedString = new StringBuilder();
		Generation[] generations = new Generation[n];
		for(int i = 0; i < n; i++) {
			String name = getParsedNameForChild(i);
			Generator generator = getChildGenerator(name, grammar, getEntry().getSymbol());
			GeneratorHints cHints = getChildGeneratorHints(name);
			Generation gen = generator.generate(grammar, cHints); // Rule.generate(grammar, children[0]);
			gen.setName(name);
			generatedString.append(gen);
			generations[i] = gen;
		}
		return new Generation(generatedString.toString(), generations);
	};

	@Override
	public Generator getDefaultGenerator() {
		return DEFAULT_GENERATOR;
	}
}
