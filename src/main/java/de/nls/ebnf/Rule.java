package de.nls.ebnf;

import de.nls.Autocompleter;
import de.nls.Evaluator;
import de.nls.core.BNF;
import de.nls.core.NonTerminal;
import de.nls.core.Symbol;

public abstract class Rule {
	protected final String type;
	protected final NonTerminal tgt;
	protected final Symbol[] children;
	protected String[] parsedChildNames;

	private Evaluator evaluator;
	private Autocompleter autocompleter;
	private ParseListener onSuccessfulParsed;

	public Rule(String type, NonTerminal tgt, Symbol... children) {
		this.type = type;
		this.tgt = tgt != null
				? tgt
				: new NonTerminal(type + ":" + NonTerminal.makeRandomSymbol());
		this.children = children;
	}

	public NonTerminal getTarget() {
		return tgt;
	}

	public Symbol[] getChildren() {
		return children;
	}

	public Evaluator getEvaluator() {
		return evaluator;
	}

	public Rule setEvaluator(Evaluator evaluator) {
		this.evaluator = evaluator;
		return this;
	}

	public Autocompleter getAutocompleter() {
		return autocompleter;
	}

	public Rule setAutocompleter(Autocompleter autocompleter) {
		this.autocompleter = autocompleter;
		return this;
	}

	public Rule onSuccessfulParsed(ParseListener listener) {
		this.onSuccessfulParsed = listener;
		return this;
	}

	public ParseListener getOnSuccessfulParsed() {
		return this.onSuccessfulParsed;
	}

	public static EBNFProduction addProduction(BNF grammar, Rule rule, NonTerminal left, Symbol... right) {
		EBNFProduction production = new EBNFProduction(rule, left, right);
		grammar.addProduction(production);
		return production;
	}

	public String getNameForChild(int idx) {
		if(parsedChildNames == null) {
			return null;
		}
		if(parsedChildNames.length == 1)
			return parsedChildNames[0];
		if(idx >= parsedChildNames.length)
			return "no name";
		return parsedChildNames[idx];
	}

	public void setParsedChildNames(String... parsedChildNames) {
		this.parsedChildNames = parsedChildNames;
	}

	public abstract void createBNF(BNF grammar);
}
