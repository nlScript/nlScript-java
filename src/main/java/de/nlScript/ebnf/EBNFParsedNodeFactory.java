package de.nlScript.ebnf;

import de.nlScript.core.DefaultParsedNode;
import de.nlScript.ParsedNode;
import de.nlScript.core.Matcher;
import de.nlScript.core.ParsedNodeFactory;
import de.nlScript.core.Production;
import de.nlScript.core.Symbol;

public class EBNFParsedNodeFactory implements ParsedNodeFactory {


	public static final EBNFParsedNodeFactory INSTANCE = new EBNFParsedNodeFactory();

	private EBNFParsedNodeFactory() {}

	@Override
	public DefaultParsedNode createNode(Matcher matcher, Symbol symbol, Production production) {
		return new ParsedNode(matcher, symbol, production);
	}
}
