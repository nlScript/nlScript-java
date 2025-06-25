package nlScript.ebnf;

import nlScript.Autocompleter;
import nlScript.Evaluator;
import nlScript.core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public abstract class Rule implements RepresentsSymbol {
	protected final String type;
	protected final NonTerminal tgt;
	protected final Named<?>[] children;
	protected String[] parsedChildNames;

	private Evaluator evaluator;
	private Autocompleter autocompleter;
	private ParseListener onSuccessfulParsed;

	protected final ArrayList<EBNFProduction> productions = new ArrayList<>();

	public Rule(String type, NonTerminal tgt, Named<?>... children) {
		this.type = type;

		if(tgt != null)
			this.tgt = tgt;
		else {
			String t = type;
			if(children.length == 1)
				t += "(" + children[0].getSymbol() + ")";
			t += ":" + NonTerminal.makeRandomSymbol();
			this.tgt = new NonTerminal(t);
		}
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

	public Named<?>[] getChildren() {
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

	public int getChildIndexForName(String name) {
		for(int i = 0; i < children.length; i++) {
			if(children[i].getName().equals(name))
				return i;
		}
		return -1;
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

	public String getParsedNameForChild(int idx) {
		if(parsedChildNames != null) {
			if (parsedChildNames.length == 1)
				return parsedChildNames[0];
			if (idx >= parsedChildNames.length)
				return "no name";
			return parsedChildNames[idx];
		}
		// only one child, but idx > 0
		if(children.length == 1)
			return children[0].getName();
		if(idx < children.length)
			return children[idx].getName();
		return "no name";
	}

	protected static Symbol[] getSymbols(Named<?>... named) {
		Symbol[] ret = new Symbol[named.length];
		for(int i = 0; i < named.length; i++)
			ret[i] = named[i].getSymbol();
		return ret;
	}

	public void setParsedChildNames(String... parsedChildNames) {
		this.parsedChildNames = parsedChildNames;
	}

	public abstract void createBNF(BNF grammar);

	public boolean hasParsedName(String name) {
		if(parsedChildNames == null) {
			for(Named<?> n : children)
				if(n.getName().equals(name))
					return true;
			return false;
		}
		for(String parsedName : parsedChildNames)
			if(parsedName.equals(name))
				return true;
		return false;
	}

}
