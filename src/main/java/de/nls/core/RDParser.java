package de.nls.core;

import de.nls.Autocompleter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class RDParser {

	private final ParsedNodeFactory parsedNodeFactory;

	private final BNF grammar;
	private final Lexer lexer;

	public RDParser(BNF grammar, Lexer lexer, ParsedNodeFactory parsedNodeFactory) {
		this.grammar = grammar;
		this.lexer = lexer;
		this.parsedNodeFactory = parsedNodeFactory;
	}

	public DefaultParsedNode parse() {
		return parse(null);
	}

	public DefaultParsedNode parse(ArrayList<Autocompletion> autocompletions) {
		SymbolSequence seq = new SymbolSequence(BNF.ARTIFICIAL_START_SYMBOL);
		SymbolSequence parsedSequence = parse(seq, autocompletions);
//		if(autocompletions != null && autocompletions.size() == 1 && autocompletions.get(0) == null)
//			autocompletions.clear();
		if(autocompletions != null && autocompletions.size() > 0 && autocompletions.get(autocompletions.size() - 1) == null)
			autocompletions.remove(autocompletions.size() - 1);
		DefaultParsedNode[] last = new DefaultParsedNode[1];
		DefaultParsedNode ret = createParsedTree(parsedSequence, last);
//		if(autocompletions != null)
//			System.out.println("Autocompletions: " + autocompletions);
		return ret;
	}

	public DefaultParsedNode buildAst(DefaultParsedNode pn) {
		DefaultParsedNode[] children = new DefaultParsedNode[pn.numChildren()];
		for(int i = 0; i < pn.numChildren(); i++) {
			children[i] = buildAst(pn.getChild(i));
		}
		pn.removeAllChildren();
		if(pn.getProduction() != null)
			pn.getProduction().builtAST(pn, children);

		return pn;
	}

	/**
	 * Removes all entries and puts null at index 0 if further autocompletion should be prohibited.
	 */
	private void addAutocompletions(SymbolSequence symbolSequence, ArrayList<Autocompletion> autocompletions) {
		assert autocompletions != null;
		// if(autocompletions.size() == 1 && autocompletions.get(0) == null)
		if(autocompletions.size() > 0 && autocompletions.get(autocompletions.size() - 1) == null)
			return;

		DefaultParsedNode[] last = new DefaultParsedNode[1];
		createParsedTree(symbolSequence, last);

		// get a trace to the root
		ArrayList<DefaultParsedNode> pathToRoot = new ArrayList<>();
		DefaultParsedNode parent = last[0];
		while(parent != null) {
			pathToRoot.add(parent);
			parent = parent.getParent();
		}

		// find the node closest to root which provides autocompletion
		DefaultParsedNode autocompletingParent = null;
		for(int i = pathToRoot.size() - 1; i >= 0; i--) {
			DefaultParsedNode tmp = pathToRoot.get(i);
			if (tmp.doesAutocomplete()) {
				autocompletingParent = tmp;
				break;
			}
		}
		if(autocompletingParent == null)
			return;

		int autocompletingParentStart = autocompletingParent.getMatcher().pos;
		String alreadyEntered = lexer.substring(autocompletingParentStart);
		String completion = autocompletingParent.getAutocompletion();
		if(completion != null && !completion.isEmpty()) {
			for(String c : completion.split(";;;")) {
				if(c.equals(Autocompleter.VETO)) {
					// autocompletions.clear();
					autocompletions.add(null); // to prevent further autocompletion
					return;
				}
				Autocompletion ac = new Autocompletion(c, alreadyEntered);
				if(!autocompletions.contains(ac))
					autocompletions.add(ac);
			}
		}
	}

	/**
	 * algorithm:
	 *
	 * - check the next symbol in the symbol sequence:
	 *   - while it's a terminal, match it against the lexer:
	 *     - if it does not match, return false
	 *     - if it matches, increment the current position in the symbol sequence and int the lexer
	 *       - if the lexer is at the end (or the current symbol sequence), return true
	 *   - the next symbol is non-terminal U
	 *   - for all productions U -> XYZ
	 *     - in the symbol sequence, replace U with XYZ
	 */
	private SymbolSequence parse(SymbolSequence symbolSequence, ArrayList<Autocompletion> autocompletions) {
		Symbol next = symbolSequence.getCurrentSymbol();

		while(next.isTerminal()) {
			Matcher matcher = ((Terminal) next).matches(lexer);
			symbolSequence.addMatcher(matcher);
			if(matcher.state == ParsingState.END_OF_INPUT && autocompletions != null)
				addAutocompletions(symbolSequence, autocompletions);

			if(matcher.state != ParsingState.SUCCESSFUL)
				return symbolSequence;
			symbolSequence.incrementPosition();
			lexer.fwd(matcher.parsed.length());
			if(lexer.isDone())
				return symbolSequence;
			next = symbolSequence.getCurrentSymbol();
		}
		NonTerminal u = (NonTerminal) next;
		ArrayList<Production> alternates = grammar.getProductions(u);
		SymbolSequence best = null;
		int lexerPosOfBest = lexer.getPosition();
		for(Production alternate : alternates) {
			int lexerPos = lexer.getPosition();
			SymbolSequence nextSequence = symbolSequence.replaceCurrentSymbol(alternate);
			SymbolSequence parsedSequence = parse(nextSequence, autocompletions);
			Matcher m = parsedSequence.getLastMatcher();
			if(m.state == ParsingState.SUCCESSFUL)
				return parsedSequence;
			if(best == null || m.isBetterThan(best.getLastMatcher())) {
				best = parsedSequence;
				lexerPosOfBest = lexer.getPosition();
			}
			lexer.setPosition(lexerPos);
		}
		if(best != null) {
			lexer.setPosition(lexerPosOfBest);
		}
		return best;
	}

	protected DefaultParsedNode createParsedTree(SymbolSequence leafSequence, DefaultParsedNode[] retLast) {
		LinkedList<DefaultParsedNode> parsedNodeSequence = new LinkedList<>();
		int nParsedMatchers = leafSequence.parsedMatchers.size();
		int i = 0;
		for(Symbol symbol : leafSequence.sequence) {
			Matcher matcher = i < nParsedMatchers
					? leafSequence.parsedMatchers.get(i)
					: new Matcher(ParsingState.NOT_PARSED, 0, "");
			i++;

			DefaultParsedNode pn = parsedNodeFactory.createNode(matcher, symbol, null);
			parsedNodeSequence.add(pn);
		}

		if(retLast != null)
			retLast[0] = parsedNodeSequence.get(nParsedMatchers - 1);

		SymbolSequence childSequence = leafSequence;
		while(childSequence.parent != null) {
			SymbolSequence parentSequence = childSequence.parent;
			Production productionToCreateChildSequence = childSequence.production;
			assert productionToCreateChildSequence != null;
			int pos = parentSequence.pos;
			Symbol[] rhs = productionToCreateChildSequence.getRight();
			Symbol   lhs = productionToCreateChildSequence.getLeft();
			int rhsSize = rhs.length;
			List<DefaultParsedNode> childList = parsedNodeSequence.subList(pos, pos + rhsSize);

			Matcher matcher = matcherFromChildSequence(childList);
			DefaultParsedNode newParent = parsedNodeFactory.createNode(matcher, lhs, productionToCreateChildSequence);
			newParent.addChildren(childList.toArray(new DefaultParsedNode[0]));
			childList.clear();
			parsedNodeSequence.add(pos, newParent);

			childSequence = childSequence.parent;
		}

		DefaultParsedNode root = parsedNodeSequence.get(0);
		assert root.getSymbol().equals(BNF.ARTIFICIAL_START_SYMBOL);

		notifyExtensionListeners(root);

		return root;
	}

	private static void notifyExtensionListeners(DefaultParsedNode pn) {
		Production production = pn.getProduction();
		if(production != null) {
			production.wasExtended(pn, pn.getChildren());
			for (DefaultParsedNode child : pn.getChildren())
				notifyExtensionListeners(child);
		}
	}

	private static Matcher matcherFromChildSequence(List<DefaultParsedNode> children) {
		int pos = children.size() > 0 ? children.get(0).getMatcher().pos : 0;
		ParsingState state = ParsingState.NOT_PARSED;
		StringBuilder parsed = new StringBuilder();
		for(DefaultParsedNode child : children) {
			// already encountered EOI or FAILED before, do nothing
			if(state == ParsingState.END_OF_INPUT || state == ParsingState.FAILED)
				break;

			Matcher matcher = child.getMatcher();
			ParsingState childState = matcher.state;
			if (childState != ParsingState.NOT_PARSED) {
				if (state == ParsingState.NOT_PARSED || !childState.isBetterThan(state)) {
					state = childState;
				}
			}
			parsed.append(matcher.parsed);
		}
		return new Matcher(state, pos, parsed.toString());
	}

	protected static class SymbolSequence {
		private final LinkedList<Symbol> sequence = new LinkedList<>();
		private int pos = 0;
		private final SymbolSequence parent;
		private final ArrayList<Matcher> parsedMatchers = new ArrayList<>();
		private final Production production;

		private SymbolSequence(SymbolSequence o, Production production) {
			this.sequence.addAll(o.sequence);
			this.pos = o.pos;
			this.parent = o;
			this.production = production;
		}

		public SymbolSequence(Symbol start) {
			sequence.add(start);
			parent = null;
			production = null;
		}

		public Matcher getLastMatcher() {
			return parsedMatchers.get(parsedMatchers.size() - 1);
		}

		public void addMatcher(Matcher matcher) {
			this.parsedMatchers.add(matcher);
		}

		public Symbol getCurrentSymbol() {
			return sequence.get(pos);
		}

		public SymbolSequence replaceCurrentSymbol(Production production) {
			SymbolSequence copy = new SymbolSequence(this, production);
			copy.parsedMatchers.addAll(this.parsedMatchers);
			copy.sequence.remove(pos);
			Symbol[] replacement = production.getRight();
			for(int i = 0; i < replacement.length; i++)
				copy.sequence.add(pos + i, replacement[i]);
			return copy;
		}

		public void incrementPosition() {
			pos++;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			int i = 0;
			for(Symbol sym : sequence) {
				if(i++ == pos)
					sb.append(".");
				sb.append(sym).append(" -- ");
			}
			return sb.toString();
		}
	}
}
