package nlScript.ebnf;

import nlScript.Autocompleter;
import nlScript.Evaluator;
import nlScript.JsonSerializer;
import nlScript.core.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public abstract class Rule implements RepresentsSymbol, Generatable {
	protected final String type;
	protected final NonTerminal tgt;
	protected final Named<?>[] children;
	protected String[] parsedChildNames;

	private Evaluator evaluator;
	private Autocompleter autocompleter;
	private JsonSerializer json;
	private ParseListener onSuccessfulParsed;
	private GenerationListener generationListener;
	private HashMap<String, GenerationListener> childGenerationListener;

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

	public JsonSerializer getJsonSerializer() {
		return json;
	}

	public Rule setJsonSerializer(JsonSerializer json) {
		this.json = json;
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

	public Rule setGenerationListener(GenerationListener listener) {
		this.generationListener = listener;
		return this;
	}

	public Rule setChildGenerationListener(String child, GenerationListener listener) {
		if(childGenerationListener == null)
			childGenerationListener = new HashMap<>();
		childGenerationListener.put(child, listener);
		return this;
	}

	public GenerationListener getGenerationListener() {
		return generationListener;
	}

	public GenerationListener getChildGenerationListener(String child) {
		if(childGenerationListener == null)
			return null;
		return childGenerationListener.get(child);
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



	private Generator generator;
	private HashMap<String, Generator> childGenerators;
	private String generationDescription;

	public String getGenerationDescription() {
		return generationDescription;
	}

	public Rule setGenerationDescription(String generationDescription) {
		this.generationDescription = generationDescription;
		return this;
	}

	@Override
	public Generation generate(EBNFCore grammar) {
		Generation generation =  getGenerator().generate(grammar);
		if(generationDescription != null)
			generation.setDescription(generation.processText(generationDescription));
		if(generationListener != null)
			generationListener.generated(generation);
		return generation;
	}

	public Generation generateChild(String childName, EBNFCore ebnf, Symbol otherwise) {
		GenerationListener listener = childGenerationListener == null ? null : childGenerationListener.get(childName);

		if(childGenerators != null && childGenerators.get(childName) != null) {
			Generator childGenerator = childGenerators.get(childName);
			Generation generation = childGenerator.generate(ebnf);
			if(listener != null)
				listener.generated(generation);
			return generation;
		}

		// no dedicated child generator:
		if(otherwise instanceof Terminal) {
			Generation generation = ((Terminal) otherwise).generate();
			if(listener != null)
				listener.generated(generation);
			return generation;
		}
		else if(otherwise instanceof NonTerminal) {
			ArrayList<Rule> rules = ebnf.getRules((NonTerminal) otherwise);
			if(rules.isEmpty())
				throw new RuntimeException("No rule exists for " + otherwise.getSymbol());
			Rule randomRule = rules.get(new Random().nextInt(rules.size()));
			Generator childGenerator = randomRule.getGenerator();
			Generation childGeneration = childGenerator.generate(ebnf);
			String generationDescription = randomRule.generationDescription; // TODO not sure if we need a childGenerationDescription
			if(generationDescription != null)
				childGeneration.setDescription(childGeneration.processText(generationDescription));
			if(listener != null)
				listener.generated(childGeneration);
			return childGeneration;
		} else {
			throw new RuntimeException("Don't know how to create a Generator for " + otherwise);
		}
	}

	public void setGenerator(Generator generator) {
		this.generator = generator;
	}

	public Generator getGenerator() {
		return this.generator != null ? this.generator : getDefaultGenerator();
	}

	public abstract Generator getDefaultGenerator();

	public void setChildGenerator(String childName, Generator generator) {
		if(childGenerators == null)
			childGenerators = new HashMap<>();
		childGenerators.put(childName, generator);
	}

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

	public abstract String rhsToString();

	public String toString() {
		return String.format("%50s", getTarget().toString()) + " -> " + rhsToString();
	}
}
