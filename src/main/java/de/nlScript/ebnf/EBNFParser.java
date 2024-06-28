package de.nlScript.ebnf;

import de.nlScript.ParsedNode;
import de.nlScript.core.BNF;
import de.nlScript.core.DefaultParsedNode;
import de.nlScript.core.Lexer;
import de.nlScript.core.RDParser;

import java.util.ArrayList;

public class EBNFParser extends RDParser {

	public EBNFParser(BNF grammar, Lexer lexer) {
		super(grammar, lexer, EBNFParsedNodeFactory.INSTANCE);
	}

	@Override
	protected DefaultParsedNode createParsedTree(SymbolSequence leafSequence, DefaultParsedNode[] retLast) {
		fireParsingStarted();
		DefaultParsedNode root = super.createParsedTree(leafSequence, retLast);
		((ParsedNode) root).notifyListeners();
		return root;
	}

	public interface ParseStartListener {
		void parsingStarted();
	}

	private final ArrayList<ParseStartListener> parseStartListeners = new ArrayList<>();

	public void addParseStartListener(ParseStartListener l) {
		parseStartListeners.add(l);
	}

	public void removeParseStartListener(ParseStartListener l) {
		parseStartListeners.remove(l);
	}

	private void fireParsingStarted() {
		for(ParseStartListener l : parseStartListeners)
			l.parsingStarted();
	}
}
