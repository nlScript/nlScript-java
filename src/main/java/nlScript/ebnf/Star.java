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

import java.util.Random;

public class Star extends Rule {
	public Star(NonTerminal tgt, Named<?> child) {
		super("star", tgt, child);
		setEvaluator(Evaluator.ALL_CHILDREN_EVALUATOR);
	}

	public Named<?> getEntry() {
		return children[0];
	}

	public void createBNF(BNF grammar) {
		final Production p1 = addProduction(grammar, this, tgt, children[0].getSymbol(), tgt);
		final Production p2 = addProduction(grammar, this, tgt);

		p1.onExtension((parent, children) -> {
			int nthEntry = ((ParsedNode)parent).getNthEntryInParent();
			ParsedNode c0 = (ParsedNode) children[0];
			ParsedNode c1 = (ParsedNode) children[1];

			c0.setNthEntryInParent(nthEntry);
			c0.setName(getParsedNameForChild(nthEntry));
			c1.setNthEntryInParent(nthEntry + 1);
			c1.setName(parent.getName());
		});

		p1.setAstBuilder((parent, children) -> {
			// collect the ParsedNode from the first child and add all children of the 2nd child
			parent.addChildren(children[0]);
			parent.addChildren(children[1].getChildren());
		});
	}

	private final Generator DEFAULT_GENERATOR = (grammar, hints) -> {
		int nMin = (int) hints.get(GeneratorHints.Key.MIN_NUMBER, 0);
		int nMax = (int) hints.get(GeneratorHints.Key.MAX_NUMBER, Integer.MAX_VALUE);
		int n = RandomInt.next(nMin, nMax);
		StringBuilder generatedString = new StringBuilder();
		Generation[] generations = new Generation[n];
		for(int i = 0; i < n; i++) {
			String name = getParsedNameForChild(i);
			Generator generator = getChildGenerator(name, grammar, getEntry().getSymbol());
			GeneratorHints cHints = getChildGeneratorHints(name);
			Generation gen = generator.generate(grammar, cHints);
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