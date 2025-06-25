package nlScript.ebnf;

import nlScript.Evaluator;
import nlScript.ParsedNode;
import nlScript.core.*;
import nlScript.util.RandomInt;
import nlScript.util.Range;

import java.util.ArrayList;
import java.util.List;

public class Join extends Rule {
	private final Named<?> open;
	private final Named<?> close;
	private final Named<?> delimiter;
	private Range cardinality;
	private boolean onlyKeepEntries = true;

	private static Named<?>[] makeChildren(Named<?> entry, Named<?> open, Named<?> close, Named<?> delimiter) {
		int n = 1;
		if(open      != null) n++;
		if(close     != null) n++;
		if(delimiter != null) n++;

		Named<?>[] ret = new Named<?>[n];
		int i = 0;
		ret[i++] = entry;
		if(open      != null) ret[i++] = open;
		if(close     != null) ret[i++] = close;
		if(delimiter != null) ret[i]   = delimiter;

		return ret;
	}

	public Join(NonTerminal tgt, Named<?> entry, Named<?> open, Named<?> close, Named<?> delimiter, Range cardinality) {
		super("join", tgt, makeChildren(entry, open, close, delimiter));
		this.open = open;
		this.close = close;
		this.delimiter = delimiter;
		this.cardinality = cardinality;
		setEvaluator(Evaluator.ALL_CHILDREN_EVALUATOR);
	}

	public Named<?> getOpen() {
		return open;
	}

	public Named<?> getClose() {
		return close;
	}

	public Named<?> getDelimiter() {
		return delimiter;
	}

	public Named<?> getEntry() {
		return children[0];
	}

	public Range getCardinality() {
		return cardinality;
	}

	public void setCardinality(Range cardinality) {
		this.cardinality = cardinality;
	}

	public void setOnlyKeepEntries(boolean onlyKeepEntries) {
		this.onlyKeepEntries = onlyKeepEntries;
	}

	public void createBNF(BNF g) {
		final Symbol first = children[0].getSymbol();
		final Named<NonTerminal> next = new NonTerminal("next-" + NonTerminal.makeRandomSymbol()).withName("next");
		final boolean hasOpen = open != null && !open.getSymbol().isEpsilon();
		final boolean hasClose = close != null && !close.getSymbol().isEpsilon();
		final boolean hasDelimiter = delimiter != null && !delimiter.getSymbol().isEpsilon();
		if(hasDelimiter) {
			final Production p = addProduction(g, this, next.get(), delimiter.getSymbol(), first);

			/*
			 * seq +-- open
			 *     |-- repetition +-- first (0th name)
			 *     |              |-- [              ] +-- next +-- delimiter
			 *     |                                   |        |-- entry (1st name)
			 *     |                                   |-- next +-- delimiter
			 *     |                                   |        |-- entry (2nd name)
			 *     |                                   |-- next +-- delimiter
			 *     |                                            |-- entry (3rd name)
			 *     |-- close
			 */
			p.onExtension((parent, children) -> {
				int nthEntry = ((ParsedNode)parent).getNthEntryInParent() + 1; // increment because next starts with index 1, index 0 is first
				children[0].setName("delimiter");
				children[1].setName(getParsedNameForChild(nthEntry));
			});

			if(onlyKeepEntries)
				p.setAstBuilder((parent, children) -> parent.addChildren(children[1]));
			else
				p.setAstBuilder(Production.AstBuilder.DEFAULT);
		}
		else {
			Production p = addProduction(g, this, next.get(), first);
			p.onExtension((parent, children) -> {
				int nthEntry = ((ParsedNode)parent).getNthEntryInParent() + 1; // increment because next starts with index 1, index 0 is first
				children[0].setName(getParsedNameForChild(nthEntry));
			});
			p.setAstBuilder((parent, children) -> parent.addChildren(children[0]));
		}

		// Assume L -> first next
		Production.AstBuilder astBuilder = ((parent, children) -> {
			parent.addChildren(children[0]);
			for(DefaultParsedNode pn : children[1].getChildren())
				parent.addChildren(pn.getChildren());
		});

		NonTerminal repetition = new NonTerminal("repetition:" + NonTerminal.makeRandomSymbol());

		// + : L -> first next*
		if(cardinality.equals(Range.PLUS)) {
			Star star = new Star(null, next);
			star.createBNF(g);
			productions.addAll(star.productions);
			Production p = addProduction(g, this, repetition, first, star.tgt);
			p.onExtension((parent, children) -> {
				children[0].setName(getParsedNameForChild(0));
				children[1].setName("star");
			});
			p.setAstBuilder(astBuilder);
		}
		// L -> first L
		// L -> next L
		// L -> e

		// * : L -> first next*
		//     L -> epsilon
		else if(cardinality.equals(Range.STAR)) {
			Star star = new Star(null, next);
			star.createBNF(g);
			productions.addAll(star.productions);

			Production p1 = addProduction(g, this, repetition, first, star.tgt);
			Production p2 = addProduction(g, this, repetition, Terminal.EPSILON);
			p1.setAstBuilder(astBuilder);
			p2.setAstBuilder((parent, children) -> {});

			p1.onExtension((parent, children) -> {
				children[0].setName(getParsedNameForChild(0));
				children[1].setName("star");
			});
		}

		// ? : L -> first
		//     L -> epsilon
		else if(cardinality.equals(Range.OPTIONAL)) {
			Production p1 = addProduction(g, this, repetition, first); // using default ASTBuilder
			p1.onExtension((parent, children) -> children[0].setName(getParsedNameForChild(0)));
			Production p2 = addProduction(g, this, repetition, Terminal.EPSILON);
			p2.setAstBuilder((parent, children) -> {});
		}

		// Dealing with a specific range
		else {
			int lower = cardinality.getLower();
			int upper = cardinality.getUpper();
			if(lower == 0 && upper == 0) {
				addProduction(g, this, repetition, Terminal.EPSILON).setAstBuilder((parent, children) -> {});
			}
			else if(lower == 1 && upper == 1) {
				Production p = addProduction(g, this, repetition, first); // using default ASTBuilder
				p.onExtension(((parent, children) -> children[0].setName(getParsedNameForChild(0))));
			}
			else {
				if(lower <= 0) {
					Repeat repeat = new Repeat(null, next, 0, upper - 1);
					repeat.createBNF(g);
					productions.addAll(repeat.productions);
					Production p = addProduction(g, this, repetition, first, repeat.tgt);
					p.setAstBuilder(astBuilder);
					p.onExtension((parent, children) -> {
						children[0].setName(getParsedNameForChild(0));
						children[1].setName("repeat");
					});
					addProduction(g, this, repetition, Terminal.EPSILON).setAstBuilder(((parent, children) -> {}));
				}
				else {
					Repeat repeat = new Repeat(null, next, lower - 1, upper - 1);
					repeat.createBNF(g);
					productions.addAll(repeat.productions);
					Production p = addProduction(g, this, repetition, first, repeat.tgt);
					p.setAstBuilder(astBuilder);
					p.onExtension((parent, children) -> {
						children[0].setName(getParsedNameForChild(0));
						children[1].setName("repeat");
					});
				}
			}
		}

		if(!hasOpen && !hasClose) {
			Production p = addProduction(g, this, tgt, repetition);
			p.onExtension(((parent, children) -> children[0].setName("repetition")));
			p.setAstBuilder(((parent, children) -> parent.addChildren(children[0].getChildren())));
		}
		else {
			Production p = addProduction(g, this, tgt, open.getSymbol(), repetition, close.getSymbol());
			p.onExtension((parent, children) -> {
				if(!onlyKeepEntries)
					children[0].setName("open");
				children[1].setName("repetition");
				if(!onlyKeepEntries)
					children[2].setName("close");
			});
			p.setAstBuilder((parent, children) -> {
				if(!onlyKeepEntries)
					parent.addChildren(children[0]);
				parent.addChildren(children[1].getChildren());
				if(!onlyKeepEntries)
					parent.addChildren(children[2]);
			});
		}
	}

	private final Generator DEFAULT_GENERATOR = (grammar, hints) -> {
		final boolean hasOpen = getOpen() != null && !getOpen().getSymbol().isEpsilon();
		final boolean hasClose = getClose() != null && !getClose().getSymbol().isEpsilon();
		final boolean hasDelimiter = getDelimiter() != null && !getDelimiter().getSymbol().isEpsilon();

		int lower = getCardinality().getLower();
		int upper = getCardinality().getUpper();
		if(upper == Integer.MAX_VALUE)
			upper = (int) hints.get(GeneratorHints.Key.MAX_NUMBER, upper);
		int n = RandomInt.next(lower, upper);
		StringBuilder generatedString = new StringBuilder();
		List<Generation> generations = new ArrayList<>();
		if(hasOpen) {
			Generator generator = getChildGenerator(getOpen().getName(), grammar, getOpen().getSymbol());
			GeneratorHints cHints = getChildGeneratorHints(getOpen().getName());
			Generation gen = generator.generate(grammar, cHints); // generate(grammar, getOpen());
			gen.setName(getOpen().getName());
			generatedString.append(gen);
			generations.add(gen);
		}
		for(int i = 0; i < n; i++) {
			String name = getParsedNameForChild(i);
			Generator generator = getChildGenerator(name, grammar, getEntry().getSymbol());
			GeneratorHints cHints = getChildGeneratorHints(name);
			Generation gen = generator.generate(grammar, cHints); // generate(grammar, children[0]);
			gen.setName(name);
			generatedString.append(gen);
			generations.add(gen);

			if(hasDelimiter && i < n - 1) {
				generator = getChildGenerator(getDelimiter().getName(), grammar, getDelimiter().getSymbol());
				cHints = getChildGeneratorHints(getDelimiter().getName());
				gen = generator.generate(grammar, cHints);
				gen.setName(getDelimiter().getName());
				generatedString.append(gen);
				generations.add(gen);
			}
		}
		if(hasClose) {
			Generator generator = getChildGenerator(getClose().getName(), grammar, getClose().getSymbol());
			GeneratorHints cHints = getChildGeneratorHints(getClose().getName());
			Generation gen = generator.generate(grammar, cHints);
			gen.setName(getClose().getName());
			generatedString.append(gen);
			generations.add(gen);
		}
		return new Generation(generatedString.toString(), generations.toArray(generations.toArray(new Generation[0])));
	};

	@Override
	public Generator getDefaultGenerator() {
		return DEFAULT_GENERATOR;
	}
}
