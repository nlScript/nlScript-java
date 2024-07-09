package nlScript.core;

import nlScript.ParseException;
import nlScript.core.BNF;
import nlScript.core.DefaultParsedNode;
import nlScript.core.Lexer;
import nlScript.core.NonTerminal;
import nlScript.core.ParsedNodeFactory;
import nlScript.core.ParsingState;
import nlScript.core.Production;
import nlScript.core.RDParser;
import nlScript.core.Terminal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class TestRDParser {
	@Test
	public void testParse() throws ParseException {
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
