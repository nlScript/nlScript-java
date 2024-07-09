package nlScript.core;

public interface ParsedNodeFactory {

	DefaultParsedNode createNode(Matcher matcher, Symbol symbol, Production production);

	ParsedNodeFactory DEFAULT = DefaultParsedNode::new;

}
