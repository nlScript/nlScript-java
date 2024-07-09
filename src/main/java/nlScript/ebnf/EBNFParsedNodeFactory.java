package nlScript.ebnf;

import nlScript.ParsedNode;
import nlScript.core.Matcher;
import nlScript.core.ParsedNodeFactory;
import nlScript.core.Production;
import nlScript.core.Symbol;
import nlScript.core.DefaultParsedNode;

public class EBNFParsedNodeFactory implements ParsedNodeFactory {


	public static final EBNFParsedNodeFactory INSTANCE = new EBNFParsedNodeFactory();

	private EBNFParsedNodeFactory() {}

	@Override
	public DefaultParsedNode createNode(Matcher matcher, Symbol symbol, Production production) {
		return new ParsedNode(matcher, symbol, production);
	}
}
