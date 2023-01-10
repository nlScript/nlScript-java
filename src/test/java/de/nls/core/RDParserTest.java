package de.nls.core;

import de.nls.ParsedNode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RDParserTest {
	@Test
	public void testParse() {
		BNF bnf = new BNF();
		bnf.addProduction(new Production(new NonTerminal("EXPR"),
				new NonTerminal("TERM"), new Terminal.Literal("+"), new NonTerminal("EXPR")));
		bnf.addProduction(new Production(new NonTerminal("EXPR"),
				new NonTerminal("TERM")));
		bnf.addProduction(new Production(new NonTerminal("TERM"),
				new NonTerminal("FACTOR"), new Terminal.Literal("*"), new NonTerminal("FACTOR")));
		bnf.addProduction(new Production(new NonTerminal("TERM"),
				new NonTerminal("FACTOR")));
		bnf.addProduction(new Production(new NonTerminal("FACTOR"),
				BNF.DIGIT));

		bnf.addProduction(new Production(BNF.ARTIFICIAL_START_SYMBOL,
				new NonTerminal("EXPR"), BNF.ARTIFICIAL_STOP_SYMBOL));

		RDParser parser = new RDParser(bnf, new Lexer("3+4*6+8"));
		ParsedNode parsed = parser.parse();
		assertEquals(ParsingState.SUCCESSFUL, parsed.getMatcher().state);
	}
}
