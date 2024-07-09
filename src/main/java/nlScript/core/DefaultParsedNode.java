package nlScript.core;

import nlScript.Autocompleter;

import java.util.ArrayList;
import java.util.Arrays;

public class DefaultParsedNode {

	private DefaultParsedNode parent = null;
	private final ArrayList<DefaultParsedNode> children = new ArrayList<>();

	private final Symbol symbol;
	private final Production production;
	private final Matcher matcher;
	private String name;

	public DefaultParsedNode(Matcher matcher, Symbol symbol, Production production) {
		this.matcher = matcher;
		this.symbol = symbol;
		this.production = production;
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

	public Production getProduction() {
		return production;
	}

	public Matcher getMatcher() {
		return matcher;
	}

	public boolean doesAutocomplete() {
		return getAutocompletion(true) != null;
	}

	public Autocompletion[] getAutocompletion(boolean justCheck) {
		return Autocompleter.FALLBACK_AUTOCOMPLETER.getAutocompletion(this, justCheck);
	}

	public int numChildren() {
		return children.size();
	}

	public final DefaultParsedNode[] getChildren() {
		return children.toArray(new DefaultParsedNode[0]);
	}

	public DefaultParsedNode getChild(int i) {
		return children.get(i);
	}

	public DefaultParsedNode getChild(String name) {
		for(DefaultParsedNode n : children)
			if (name.equals(n.getName()))
				return n;
		return null;
	}

	public void addChildren(DefaultParsedNode... children) {
		this.children.addAll(Arrays.asList(children));
		for(DefaultParsedNode child : children)
			child.parent = this;
	}

	public DefaultParsedNode getParent() {
		return parent;
	}

	public void removeAllChildren() {
		for(DefaultParsedNode child : children)
			child.parent = null;
		children.clear();
	}

	public Object evaluate() {
		if(symbol.isTerminal())
			return ((Terminal) symbol).evaluate(getMatcher());
		return getParsedString();
	}

	public Object evaluate(int child) {
		return children.get(child).evaluate();
	}

	public Object evaluate(String... names) {
		DefaultParsedNode pn = this;
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
		DefaultParsedNode pn = this;
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
