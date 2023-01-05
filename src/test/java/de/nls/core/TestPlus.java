package de.nls.core;

import de.nls.ebnf.EBNFCore;
import de.nls.ebnf.Named;
import de.nls.ebnf.Rule;
import org.junit.jupiter.api.Test;

import static de.nls.core.ParsingState.SUCCESSFUL;
import static org.junit.jupiter.api.Assertions.*;

public class TestPlus {

	private static BNF makeGrammar() {
		EBNFCore grammar = new EBNFCore();
		Rule rule = grammar.plus("plus", Named.n(BNF.DIGIT));
		grammar.setWhatToMatch(rule.getTarget());
		return grammar.createBNF();
	}

	@Test
	public void test01() {
		testSuccess("123");
	}

	@Test
	public void test02() {
		testSuccess("1");
	}

	@Test
	public void test03() {
		testFailure("");
	}

	@Test
	public void test04() {
		testFailure("s");
	}

	private static void testSuccess(String input) {
		BNF grammar = makeGrammar();

		Lexer l = new Lexer(input);
		RDParser test = new RDParser(grammar, l);
		ParsedNode root = test.parse();
		root = test.buildAst(root);

		if(root.getMatcher().state != SUCCESSFUL)
			throw new RuntimeException();

		ParsedNode parsedStar = root.getChildren()[0];
		assertEquals(input.length(), parsedStar.numChildren());

		int i = 0;
		for(ParsedNode child : parsedStar.getChildren()) {
			assertEquals(Character.toString(input.charAt(i++)), child.getParsedString());
			assertEquals(0, child.numChildren());
		}
	}

	private static void testFailure(String input) {
		BNF grammar = makeGrammar();

		Lexer l = new Lexer(input);
		RDParser parser = new RDParser(grammar, l);
		ParsedNode root = parser.parse();

		assertNotEquals(SUCCESSFUL, root.getMatcher().state);
	}
}
