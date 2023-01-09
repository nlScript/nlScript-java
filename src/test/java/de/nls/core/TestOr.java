package de.nls.core;

import de.nls.ebnf.EBNFCore;
import de.nls.ebnf.Named;
import de.nls.ebnf.Rule;
import org.junit.jupiter.api.Test;

import static de.nls.core.ParsingState.SUCCESSFUL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class TestOr {

	private static BNF makeGrammar() {
		EBNFCore grammar = new EBNFCore();
		Rule rule = grammar.or("or",
				Named.n(BNF.literal("y")),
				Named.n(BNF.literal("n")));
		grammar.setWhatToMatch(rule.getTarget());
		return grammar.createBNF();
	}

	@Test
	public void test01() {
		testSuccess("y");
	}

	@Test
	public void test02() {
		testSuccess("n");
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

		assertEquals(SUCCESSFUL, root.getMatcher().state);

		ParsedNode parsed = root.getChildren()[0];
		assertEquals(1, parsed.numChildren());

		ParsedNode child = parsed.getChild(0);
		assertEquals(input, child.getParsedString());
		assertEquals(0, child.numChildren());

		// test evaluate
		Object evaluated = parsed.evaluate();
		assertEquals(input, evaluated);
	}

	private static void testFailure(String input) {
		BNF grammar = makeGrammar();

		Lexer l = new Lexer(input);
		RDParser parser = new RDParser(grammar, l);
		ParsedNode root = parser.parse();

		assertNotEquals(SUCCESSFUL, root.getMatcher().state);
	}
}
