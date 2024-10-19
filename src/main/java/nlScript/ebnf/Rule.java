package nlScript.ebnf;

import nlScript.Autocompleter;
import nlScript.Evaluator;
import nlScript.core.BNF;
import nlScript.core.NonTerminal;
import nlScript.core.RepresentsSymbol;
import nlScript.core.Symbol;

import java.util.ArrayList;

public abstract class Rule implements RepresentsSymbol {
	protected final String type;
	protected final NonTerminal tgt;
	protected final Symbol[] children;
	protected String[] parsedChildNames;

	private Evaluator evaluator;
	private Autocompleter autocompleter;
	private ParseListener onSuccessfulParsed;

	protected final ArrayList<EBNFProduction> productions = new ArrayList<>();

	public Rule(String type, NonTerminal tgt, Symbol... children) {
		this.type = type;
		this.tgt = tgt != null
				? tgt
				: new NonTerminal(type + ":" + NonTerminal.makeRandomSymbol());
		this.children = children;
	}

	public NamedRule withName(String name) {
		return new NamedRule(this, name);
	}

	public NamedRule withName() {
		return new NamedRule(this);
	}

	public NonTerminal getTarget() {
		return tgt;
	}

	public Symbol getRepresentedSymbol() {
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
		rule.productions.add(production);
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
