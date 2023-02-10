package de.nls.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RDParserTest {
	@Test
	public void testParse() {
		BNF bnf = new BNF();
		bnf.addProduction(new Production(new NonTerminal("EXPR"),
				new NonTerminal("TERM"), Terminal.literal("+"), new NonTerminal("EXPR")));
		bnf.addProduction(new Production(new NonTerminal("EXPR"),
				new NonTerminal("TERM")));
		bnf.addProduction(new Production(new NonTerminal("TERM"),
				new NonTerminal("FACTOR"), Terminal.literal("*"), new NonTerminal("FACTOR")));
		bnf.addProduction(new Production(new NonTerminal("TERM"),
				new NonTerminal("FACTOR")));
		bnf.addProduction(new Production(new NonTerminal("FACTOR"),
				Terminal.DIGIT));

		bnf.addProduction(new Production(BNF.ARTIFICIAL_START_SYMBOL,
				new NonTerminal("EXPR"), BNF.ARTIFICIAL_STOP_SYMBOL));

		RDParser parser = new RDParser(bnf, new Lexer("3+4*6+8"), ParsedNodeFactory.DEFAULT);
		DefaultParsedNode parsed = parser.parse();
		assertEquals(ParsingState.SUCCESSFUL, parsed.getMatcher().state);
	}
}
