package nlScript.core;

import static nlScript.core.RDParser.SymbolSequence;

public interface IParseDebugger {

	void reset(SymbolSequence start, String text);

	void nextTerminal(SymbolSequence current, Matcher matcher, SymbolSequence tip);

	void nextNonTerminal(SymbolSequence current, SymbolSequence next, SymbolSequence tip);
}
