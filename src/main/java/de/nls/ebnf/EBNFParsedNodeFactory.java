package de.nls.ebnf;

import de.nls.ParsedNode;
import de.nls.core.DefaultParsedNode;
import de.nls.core.Matcher;
import de.nls.core.ParsedNodeFactory;
import de.nls.core.Production;
import de.nls.core.Symbol;

public class EBNFParsedNodeFactory implements ParsedNodeFactory {


	public static final EBNFParsedNodeFactory INSTANCE = new EBNFParsedNodeFactory();

	private EBNFParsedNodeFactory() {}

	@Override
	public DefaultParsedNode createNode(Matcher matcher, Symbol symbol, Production production) {
		return new ParsedNode(matcher, symbol, production);
	}
}
