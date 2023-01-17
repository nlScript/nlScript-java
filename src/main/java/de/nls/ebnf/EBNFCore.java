package de.nls.ebnf;

import de.nls.core.BNF;
import de.nls.core.NonTerminal;
import de.nls.core.Symbol;
import de.nls.core.Terminal;
import de.nls.util.Range;

import java.util.ArrayList;
import java.util.HashMap;

public class EBNFCore {

	private final HashMap<String, Symbol> symbols = new HashMap<>();

	private final ArrayList<Rule> rules = new ArrayList<>();

	public Symbol getSymbol(String type) {
		return symbols.get(type);
	}

	public ArrayList<Rule> getRules(NonTerminal target) {
		ArrayList<Rule> ret = new ArrayList<>(rules);
		ret.removeIf(p -> !p.getTarget().equals(target));
		return ret;
	}

	public Rule plus(String type, Named child) {
		NonTerminal tgt = newOrExistingNonTerminal(type);
		Plus plus = new Plus(tgt, child.getSymbol());
		plus.setParsedChildNames(child.getName());
		addRule(plus);
		return plus;
	}

	public Rule star(String type, Named child) {
		NonTerminal tgt = newOrExistingNonTerminal(type);
		Star star = new Star(tgt, child.getSymbol());
		star.setParsedChildNames(child.getName());
		addRule(star);
		return star;
	}

	public Rule or(String type, Named... options) {
		NonTerminal tgt = newOrExistingNonTerminal(type);
		Or or = new Or(tgt, getSymbols(options));
		or.setParsedChildNames(getNames(options));
		addRule(or);
		return or;
	}

	public Rule optional(String type, Named child) {
		NonTerminal tgt = newOrExistingNonTerminal(type);
		Optional optional = new Optional(tgt, child.getSymbol());
		optional.setParsedChildNames(child.getName());
		addRule(optional);
		return optional;
	}

	public Rule repeat(String type, Named child, int from, int to) {
		NonTerminal tgt = newOrExistingNonTerminal(type);
		Repeat repeat = new Repeat(tgt, child.getSymbol(), from, to);
		repeat.setParsedChildNames(child.getName());
		addRule(repeat);
		return repeat;
	}

	public Rule repeat(String type, Named child, String... names) {
		NonTerminal tgt = newOrExistingNonTerminal(type);
		int n = names.length;
		Repeat repeat = new Repeat(tgt, child.getSymbol(), n, n);
		repeat.setParsedChildNames(names);
		addRule(repeat);
		return repeat;
	}

	public Rule join(String type, Named child, Symbol open, Symbol close, Symbol delimiter, Range cardinality) {
		return join(type, child, open, close, delimiter, true, cardinality);
	}

	public Rule join(String type, Named child, Symbol open, Symbol close, Symbol delimiter, boolean onlyKeepEntries, Range cardinality) {
		NonTerminal tgt = newOrExistingNonTerminal(type);
		Join join = new Join(tgt, child.getSymbol(), open, close, delimiter, cardinality);
		join.setOnlyKeepEntries(onlyKeepEntries);
		join.setParsedChildNames(child.getName());
		addRule(join);
		return join;
	}

	public Rule join(String type, Named child, Symbol open, Symbol close, Symbol delimiter, String... names) {
		return join(type, child, open, close, delimiter, true, names);
	}

	public Rule join(String type, Named child, Symbol open, Symbol close, Symbol delimiter, boolean onlyKeepEntries, String... names) {
		NonTerminal tgt = newOrExistingNonTerminal(type);
		int n = names.length;
		Join join = new Join(tgt, child.getSymbol(), open, close, delimiter, new Range(n, n));
		join.setOnlyKeepEntries(onlyKeepEntries);
		join.setParsedChildNames(names);
		addRule(join);
		return join;
	}

	public Rule list(String type, Named child) {
		Named.NamedRule wsStar = Named.n("ws*", star(null, Named.n(Terminal.WHITESPACE)));
		NonTerminal delimiter = sequence(null,
				wsStar,
				Named.n(Terminal.literal(",")),
				wsStar).tgt;
		return join(type, child, null, null, delimiter, Range.STAR);
	}

	public Rule tuple(String type, Named child, String... names) {
		Named.NamedRule wsStar = Named.n("ws*", star(null, Named.n(Terminal.WHITESPACE)));
		Symbol open      = sequence(null, Named.n("open", Terminal.literal("(")), wsStar).tgt;
		Symbol close     = sequence(null, wsStar, Named.n("close", Terminal.literal(")"))).tgt;
		Symbol delimiter = sequence(null, wsStar, Named.n("delimiter", Terminal.literal(",")), wsStar).tgt;
		return join(type, child, open, close, delimiter, names);
	}

	public Rule sequence(String type, Named... children) {
		NonTerminal tgt = newOrExistingNonTerminal(type);
		Sequence sequence = new Sequence(tgt, getSymbols(children));
		sequence.setParsedChildNames(getNames(children));
		addRule(sequence);
		return sequence;
	}

	public void setWhatToMatch(NonTerminal topLevelSymbol) {
		removeRules(BNF.ARTIFICIAL_START_SYMBOL);
		Sequence sequence = new Sequence(BNF.ARTIFICIAL_START_SYMBOL, topLevelSymbol, BNF.ARTIFICIAL_STOP_SYMBOL);
		addRule(sequence);
		sequence.setEvaluator(pn -> pn.evaluate(0));
	}

	public BNF createBNF() {
		BNF grammar = new BNF();
		for(Rule r : rules)
			r.createBNF(grammar);
		return grammar;
	}

	protected static Symbol[] getSymbols(Named... named) {
		Symbol[] ret = new Symbol[named.length];
		for(int i = 0; i < named.length; i++)
			ret[i] = named[i].getSymbol();
		return ret;
	}

	protected static String[] getNames(Named... named) {
		String[] ret = new String[named.length];
		for(int i = 0; i < named.length; i++)
			ret[i] = named[i].getName();
		return ret;
	}

	private void addRule(Rule rule) {
		if(!symbols.containsKey(rule.tgt.getSymbol()))
			symbols.put(rule.tgt.getSymbol(), rule.tgt);

		for(Symbol s : rule.children) {
			if(!s.isEpsilon() && !symbols.containsKey(s.getSymbol()))
				symbols.put(s.getSymbol(), s);
		}
		rules.add(rule);
	}

	private void removeRules(NonTerminal symbol) {
		for(int i = rules.size() - 1; i >= 0; i--)
			if(rules.get(i).tgt.equals(symbol))
				rules.remove(i);
	}

	private NonTerminal newOrExistingNonTerminal(String type) {
		if(type == null)
			return null;
		Symbol s = symbols.get(type);
		if(s == null)
			s = new NonTerminal(type);
		return (NonTerminal) s;
	}
}
