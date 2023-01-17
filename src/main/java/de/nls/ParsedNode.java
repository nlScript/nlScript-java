package de.nls;

import de.nls.core.Matcher;
import de.nls.core.ParsingState;
import de.nls.core.Production;
import de.nls.core.Symbol;
import de.nls.core.Terminal;
import de.nls.ebnf.EBNFProduction;
import de.nls.ebnf.ParseListener;
import de.nls.ebnf.Rule;

import java.util.ArrayList;
import java.util.Arrays;

public class ParsedNode {

	private ParsedNode parent = null;
	private final ArrayList<ParsedNode> children = new ArrayList<>();

	private final Symbol symbol;
	private Production production;
	private Matcher matcher;
	private String name;
	private int nthEntryInParent = 0;

	public ParsedNode(Matcher matcher, Symbol symbol) {
		this.matcher = matcher;
		this.symbol = symbol;
	}

	public Symbol getSymbol() {
		return symbol;
	}

	public String getName() {
		return name != null ? name : symbol.getSymbol();
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setNthEntryInParent(int nthEntry) {
		this.nthEntryInParent = nthEntry;
	}

	public int getNthEntryInParent() {
		return nthEntryInParent;
	}

	public Production getProduction() {
		return production;
	}

	public void setProduction(Production p) {
		this.production = p;
	}

	public Rule getRule() {
		if(production != null && production instanceof EBNFProduction)
			return ((EBNFProduction) production).getRule();
		return null;
	}

	public Matcher getMatcher() {
		return matcher;
	}

	public void setMatcher(Matcher matcher) {
		this.matcher = matcher;
	}

	public boolean doesAutocomplete() {
		return getAutocompletion() != null;
	}

	public String getAutocompletion() {
		Rule rule = getRule();
		if(rule != null && rule.getAutocompleter() != null)
			return rule.getAutocompleter().getAutocompletion(this);

		if(symbol != null && symbol instanceof Terminal.Literal)
			return symbol.getSymbol();

		if(symbol != null && symbol.isTerminal()) {
			if(getParsedString().length() > 0)
				return Autocompleter.VETO;
			return "${" + getName() + "}";
		}
		return null;
	}

	public int numChildren() {
		return children.size();
	}

	public final ParsedNode[] getChildren() {
		return children.toArray(new ParsedNode[0]);
	}

	public ParsedNode getChild(int i) {
		return children.get(i);
	}

	public ParsedNode getChild(String name) {
		for(ParsedNode n : children)
			if (name.equals(n.getName()))
				return n;
		return null;
	}

	public void addChildren(ParsedNode... children) {
		this.children.addAll(Arrays.asList(children));
		for(ParsedNode child : children)
			child.parent = this;
	}

	public ParsedNode getParent() {
		return parent;
	}

	public void populateMatcher() {
		if(children.isEmpty())
			return;
		ParsingState state = ParsingState.SUCCESSFUL;
		StringBuilder parsed = new StringBuilder(matcher.parsed);
		for(ParsedNode c : children) {
			c.populateMatcher();
			parsed.append(c.getMatcher().parsed);
			state = c.matcher.state;
			if(state != ParsingState.SUCCESSFUL)
				break;
		}
		int pos = this.children.get(0).matcher.pos;
		this.matcher = new Matcher(state, pos, parsed.toString());
		if(state == ParsingState.SUCCESSFUL) {
			Rule rule = getRule();
			if(rule != null) {
				ParseListener l = rule.getOnSuccessfulParsed();
				if(l != null)
					l.parsed(this);
			}
		}
	}

	public void removeAllChildren() {
		for(ParsedNode child : children)
			child.parent = null;
		children.clear();
	}

	public Object evaluate() {
		Rule rule = getRule();
		if(rule != null && rule.getEvaluator() != null) {
			return rule.getEvaluator().evaluate(this);
		}
		return getParsedString();
	}

	public Object evaluate(int child) {
		return children.get(child).evaluate();
	}

	public Object evaluate(String... names) {
		ParsedNode pn = this;
		for(String name : names) {
			pn = pn.getChild(name);
			if (pn == null)
				return null;
		}
		return pn.evaluate();
	}

	public String getParsedString() {
		return matcher.parsed;
	}

	public String getParsedString(String... names) {
		ParsedNode pn = this;
		for(String name : names) {
			pn = pn.getChild(name);
			if(pn == null)
				return "";
		}
		return pn.getParsedString();
	}

	@Override
	public String toString() {
		return getParsedString();
	}
}
