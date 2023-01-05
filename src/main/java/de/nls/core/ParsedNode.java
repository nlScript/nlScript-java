package de.nls.core;

import java.util.ArrayList;
import java.util.Arrays;

public class ParsedNode {

	private ParsedNode parent = null;
	private final ArrayList<ParsedNode> children = new ArrayList<>();

	private final Symbol symbol;
	private Production production;
	private Matcher matcher;

	public ParsedNode(Matcher matcher, Symbol symbol) {
		this.matcher = matcher;
		this.symbol = symbol;
	}

	public Symbol getSymbol() {
		return symbol;
	}

	public void setProduction(Production p) {
		this.production = p;
	}

	public Matcher getMatcher() {
		return matcher;
	}

	public void setMatcher(Matcher matcher) {
		this.matcher = matcher;
	}

	public int numChildren() {
		return children.size();
	}

	public final ParsedNode[] getChildren() {
		return children.toArray(new ParsedNode[0]);
	}

	void addChildren(ParsedNode... children) {
		this.children.addAll(Arrays.asList(children));
		for(ParsedNode child : children)
			child.parent = this;
	}

	void populateMatcher() {
		if(children.isEmpty())
			return;
		ParsingState state = ParsingState.SUCCESSFUL;
		StringBuilder parsed = new StringBuilder(matcher.parsed);
		for(ParsedNode c : children) {
			parsed.append(c.getMatcher().parsed);
			state = c.matcher.state;
			if(state != ParsingState.SUCCESSFUL)
				break;
		}
		int pos = this.children.get(0).matcher.pos;
		this.matcher = new Matcher(state, pos, parsed.toString());
	}

	public void removeAllChildren() {
		for(ParsedNode child : children)
			child.parent = null;
		children.clear();
	}

	public String getParsedString() {
		return matcher.parsed;
	}

	@Override
	public String toString() {
		return getParsedString();
	}
}
