package de.nls.ebnf;

import de.nls.Evaluator;
import de.nls.core.BNF;
import de.nls.core.NonTerminal;
import de.nls.ParsedNode;
import de.nls.core.Production;
import de.nls.core.Symbol;
import de.nls.util.Range;

public class Join extends Rule {
	private final Symbol open;
	private final Symbol close;
	private final Symbol delimiter;
	private final Range cardinality;
	private boolean onlyKeepEntries = true;

	public Join(NonTerminal tgt, Symbol entry, Symbol open, Symbol close, Symbol delimiter, Range cardinality) {
		super("join", tgt, entry);
		this.open = open;
		this.close = close;
		this.delimiter = delimiter;
		this.cardinality = cardinality;
		setEvaluator(Evaluator.ALL_CHILDREN_EVALUATOR);
	}

	public void setOnlyKeepEntries(boolean onlyKeepEntries) {
		this.onlyKeepEntries = onlyKeepEntries;
	}

	public void createBNF(BNF g) {
		final Symbol first = children[0];
		final NonTerminal next = new NonTerminal("next-" + NonTerminal.makeRandomSymbol());
		final boolean hasOpen = open != null && !open.isEpsilon();
		final boolean hasClose = close != null && !close.isEpsilon();
		final boolean hasDelimiter = delimiter != null && !delimiter.isEpsilon();
		if(hasDelimiter) {
			final Production p = addProduction(g, this, next, delimiter, first);

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
				int nthEntry = parent.getNthEntryInParent() + 1; // increment because next starts with index 1, index 0 is first
				children[0].setName("delimiter");
				children[1].setName(getNameForChild(nthEntry));
			});

			if(onlyKeepEntries)
				p.setAstBuilder((parent, children) -> parent.addChildren(children[1]));
			else
				p.setAstBuilder(ParsedNode::addChildren);
		}
		else {
			Production p = addProduction(g, this, next, first);
			p.onExtension((parent, children) -> {
				int nthEntry = parent.getNthEntryInParent() + 1; // increment because next starts with index 1, index 0 is first
				children[0].setName(getNameForChild(nthEntry));
			});
			p.setAstBuilder((parent, children) -> parent.addChildren(children[0]));
		}

		// Assume L -> first next
		Production.AstBuilder astBuilder = ((parent, children) -> {
			parent.addChildren(children[0]);
			for(ParsedNode pn : children[1].getChildren())
				parent.addChildren(pn.getChildren());
		});

		NonTerminal repetition = new NonTerminal("repetition:" + NonTerminal.makeRandomSymbol());

		// + : L -> first next*
		if(cardinality.equals(Range.PLUS)) {
			Star star = new Star(null, next);
			star.setParsedChildNames("next");
			star.createBNF(g);
			Production p = addProduction(g, this, repetition, first, star.tgt);
			p.onExtension((parent, children) -> {
				children[0].setName(getNameForChild(0));
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
			star.setParsedChildNames("next");
			star.createBNF(g);

			Production p1 = addProduction(g, this, repetition, first, star.tgt);
			Production p2 = addProduction(g, this, repetition, BNF.EPSILON);
			p1.setAstBuilder(astBuilder);
			p2.setAstBuilder((parent, children) -> {});

			p1.onExtension((parent, children) -> {
				children[0].setName(getNameForChild(0));
				children[1].setName("star");
			});
		}

		// ? : L -> first
		//     L -> epsilon
		else if(cardinality.equals(Range.OPTIONAL)) {
			Production p1 = addProduction(g, this, repetition, first); // using default ASTBuilder
			p1.onExtension((parent, children) -> children[0].setName(getNameForChild(0)));
			Production p2 = addProduction(g, this, repetition, BNF.EPSILON);
			p2.setAstBuilder((parent, children) -> {});
		}

		// Dealing with a specific range
		else {
			int lower = cardinality.getLower();
			int upper = cardinality.getUpper();
			if(lower == 0 && upper == 0) {
				addProduction(g, this, repetition, BNF.EPSILON).setAstBuilder((parent, children) -> {});
			}
			else if(lower == 1 && upper == 1) {
				Production p = addProduction(g, this, repetition, first); // using default ASTBuilder
				p.onExtension(((parent, children) -> children[0].setName(getNameForChild(0))));
			}
			else {
				if(lower <= 0) {
					Repeat repeat = new Repeat(null, next, 0, upper - 1);
					repeat.setParsedChildNames("next");
					repeat.createBNF(g);
					Production p = addProduction(g, this, repetition, first, repeat.tgt);
					p.setAstBuilder(astBuilder);
					p.onExtension((parent, children) -> {
						children[0].setName(getNameForChild(0));
						children[1].setName("repeat");
					});
					addProduction(g, this, repetition, BNF.EPSILON).setAstBuilder(((parent, children) -> {}));
				}
				else {
					Repeat repeat = new Repeat(null, next, lower - 1, upper - 1);
					repeat.setParsedChildNames("next");
					repeat.createBNF(g);
					Production p = addProduction(g, this, repetition, first, repeat.tgt);
					p.setAstBuilder(astBuilder);
					p.onExtension((parent, children) -> {
						children[0].setName(getNameForChild(0));
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
			Production p = addProduction(g, this, tgt, open, repetition, close);
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
}
