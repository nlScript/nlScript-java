package de.nls.ebnf;

import de.nls.core.BNF;
import de.nls.core.NonTerminal;
import de.nls.core.Symbol;

import java.util.ArrayList;
import java.util.HashMap;

public class EBNFCore {

	private final HashMap<String, Symbol> symbols = new HashMap<>();

	private final ArrayList<Rule> rules = new ArrayList<>();


	public Rule plus(String type, Named child) {
		NonTerminal tgt = newOrExistingNonTerminal(type);
		Plus plus = new Plus(tgt, child.getSymbol());
		plus.setParsedChildNames(child.getName());
		addRule(plus);
		return plus;
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
