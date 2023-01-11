package de.nls.core;

import de.nls.Autocompleter;
import de.nls.ParsedNode;

import java.util.ArrayList;
import java.util.LinkedList;

public class RDParser {
	private final BNF grammar;
	private final Lexer lexer;

	public RDParser(BNF grammar, Lexer lexer) {
		this.grammar = grammar;
		this.lexer = lexer;
	}

	public ParsedNode parse() {
		return parse(new ArrayList<>());
	}

	public ParsedNode parse(ArrayList<Autocompletion> autocompletions) {
		SymbolSequence seq = new SymbolSequence(BNF.ARTIFICIAL_START_SYMBOL);
		ParsedNode ret = seq.getCurrentSymbol();
		parse(seq, autocompletions);
		populateParsedTree(ret);
		return ret;
	}

	public ParsedNode buildAst(ParsedNode pn) {
		ParsedNode[] children = new ParsedNode[pn.numChildren()];
		for(int i = 0; i < pn.numChildren(); i++) {
			children[i] = buildAst(pn.getChild(i));
		}
		pn.removeAllChildren();
		if(pn.getProduction() != null)
			pn.getProduction().builtAST(pn, children);

		return pn;
	}

	private void addAutocompletions(ParsedNode pn, ArrayList<Autocompletion> autocompletions) {
		// get a trace to the root
		ArrayList<ParsedNode> pathToRoot = new ArrayList<>();
		ParsedNode parent = pn;
		while(parent != null) {
			pathToRoot.add(parent);
			parent = parent.getParent();
		}

		// find the node closest to root which provides autocompletion
		ParsedNode autocompletingParent = null;
		for(int i = pathToRoot.size() - 1; i >= 0; i--) {
			ParsedNode tmp = pathToRoot.get(i);
			if (tmp.doesAutocomplete()) {
				autocompletingParent = tmp;
				break;
			}
		}
		if(autocompletingParent == null)
			return;

		autocompletingParent.populateMatcher();
		int autocompletingParentStart = autocompletingParent.getMatcher().pos;
		String alreadyEntered = lexer.substring(autocompletingParentStart);
		String completion = autocompletingParent.getAutocompletion();
		if(completion != null && !completion.isEmpty()) {
			for(String c : completion.split(";;;")) {
				if(c.equals(Autocompleter.VETO)) {
					autocompletions.clear();
					return; // TODO prevent somehow further autocompletion
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
	private ParsedNode parse(SymbolSequence symbolSequence, ArrayList<Autocompletion> autocompletions) {
		ParsedNode parent = symbolSequence.getCurrentSymbol();
		Symbol next = parent.getSymbol();
		while(next.isTerminal()) {
			Matcher matcher = ((Terminal) next).matches(lexer);
			parent.setMatcher(matcher);
			if(matcher.state == ParsingState.END_OF_INPUT)
				addAutocompletions(parent, autocompletions);
			if(matcher.state != ParsingState.SUCCESSFUL)
				return parent;
			symbolSequence.incrementPosition();
			lexer.fwd(matcher.parsed.length());
			if(lexer.isDone())
				return parent;
			parent = symbolSequence.getCurrentSymbol();
			next = parent.getSymbol();
		}
		NonTerminal u = (NonTerminal) next;
		ArrayList<Production> alternates = grammar.getProductions(u);
		ParsedNode best = null;
		ParsedNode[] childrenOfBest = null;
		for(Production alternate : alternates) {
			int lexerPos = lexer.getPosition();
			SymbolSequence nextSequence = symbolSequence.replaceCurrentSymbol(alternate);
			ParsedNode pn = parse(nextSequence, autocompletions);
			if(pn.getMatcher().state == ParsingState.SUCCESSFUL)
				return pn;
			if(best == null || pn.getMatcher().isBetterThan(best.getMatcher())) {
				best = pn;
				childrenOfBest = parent.getChildren();
			}
			parent.removeAllChildren();
			lexer.setPosition(lexerPos);
		}
		if(childrenOfBest == null) {
			System.out.println("children of best are null");
		}
		parent.addChildren(childrenOfBest);
		return best;
	}

	private void populateParsedTree(ParsedNode pn) {
		ParsedNode[] children = pn.getChildren();
		for(ParsedNode child : children)
			populateParsedTree(child);

		pn.populateMatcher();
	}

	private static class SymbolSequence {
		private final LinkedList<ParsedNode> sequence = new LinkedList<>();
		private int pos = 0;

		private SymbolSequence(SymbolSequence o) {
			this.sequence.addAll(o.sequence);
			this.pos = o.pos;
		}

		public SymbolSequence(Symbol start) {
			sequence.add(new ParsedNode(new Matcher(ParsingState.SUCCESSFUL, 0, ""), start));
		}

		public ParsedNode getCurrentSymbol() {
			return sequence.get(pos);
		}

		public SymbolSequence replaceCurrentSymbol(Production production) {
			SymbolSequence copy = new SymbolSequence(this);
			ParsedNode parent = copy.sequence.remove(pos);
			parent.setProduction(production);
			Symbol[] replacement = production.getRight();
			for(int i = 0; i < replacement.length; i++) {
				ParsedNode pn = new ParsedNode(new Matcher(ParsingState.SUCCESSFUL, 0, ""), replacement[i]);
				parent.addChildren(pn);
				copy.sequence.add(pos + i, pn);
			}
			production.wasExtended(parent, parent.getChildren());
			return copy;
		}

		public void incrementPosition() {
			pos++;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			for(ParsedNode pn : sequence) {
				sb.append(pn.getSymbol()).append(" ");
			}
			return sb.toString();
		}
	}
}
