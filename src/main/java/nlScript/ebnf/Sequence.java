package nlScript.ebnf;

import nlScript.ParsedNode;
import nlScript.core.BNF;
import nlScript.core.Generation;
import nlScript.core.Generator;
import nlScript.core.GeneratorHints;
import nlScript.core.Named;
import nlScript.core.NonTerminal;
import nlScript.core.Production;

public class Sequence extends Rule {

	private final SequenceGenerator defaultGenerator;

	public Sequence(NonTerminal tgt, Named<?>... children) {
		super("sequence", tgt, children);
		this.defaultGenerator = new SequenceGenerator(this);
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

	public static class SequenceGenerator implements Generator {
		protected final Rule sequence;

		public SequenceGenerator(Rule sequence) {
			this.sequence = sequence;
		}

		@Override
		public Generation generate(EBNFCore grammar, GeneratorHints hints) {
			int n = sequence.getChildren().length;
			StringBuilder generatedString = new StringBuilder();
			Generation[] generations = new Generation[n];
			for(int i = 0; i < n; i++) {
				String name = sequence.getParsedNameForChild(i);
				Generation gen = sequence.generateChild(name, grammar, sequence.getChildren()[i].getSymbol());
				gen.setName(name);
				generatedString.append(gen);
				generations[i] = gen;
			}
			return new Generation(generatedString.toString(), generations);
		}
	}

	@Override
	public Generator getDefaultGenerator() {
		return defaultGenerator;
	}
}
