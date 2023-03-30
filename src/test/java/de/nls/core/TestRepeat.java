package de.nls.core;

import de.nls.ebnf.EBNFCore;
import de.nls.ebnf.EBNFParsedNodeFactory;
import de.nls.ebnf.Named;
import de.nls.ebnf.Rule;
import org.junit.jupiter.api.Test;

import static de.nls.core.ParsingState.SUCCESSFUL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class TestRepeat {
	private static BNF makeGrammar(int lower, int upper) {
		EBNFCore grammar = new EBNFCore();
		Rule rule = grammar.repeat("repeat",
				Named.n("seq", grammar.sequence("seq",
						Named.n(Terminal.DIGIT),
						Named.n(Terminal.LETTER))),
				lower,
				upper);
		grammar.compile(rule.getTarget());
		return grammar.getBNF();
	}

	@Test
	public void test01() {
		BNF g = makeGrammar(1, 1);
		testSuccess(g, "1a");
		testFailure(g, "");
		testFailure(g, "1a1a");
		testFailure(g, "s");
	}

	@Test
	public void test02() {
		BNF g = makeGrammar(0, 1);
		testSuccess(g, "1a");
		testSuccess(g, "");
		testFailure(g, "1a1a");
		testFailure(g, "s");
	}

	@Test
	public void test03() {
		BNF g = makeGrammar(0, 0);
		testFailure(g, "1a");
		testSuccess(g, "");
		testFailure(g, "1a1a");
		testFailure(g, "s");
	}

	@Test
	public void test04() {
		BNF g = makeGrammar(1, 3);
		testFailure(g, "");
		testSuccess(g, "1a");
		testSuccess(g, "1a2a");
		testSuccess(g, "1a2a3a");
		testFailure(g, "1a2a3a4a");
		testFailure(g, "s");
	}

	@Test
	public void test05() {
		BNF g = makeGrammar(0, 3);
		testSuccess(g, "");
		testSuccess(g, "1a");
		testSuccess(g, "1a2a");
		testSuccess(g, "1a2a3a");
		testFailure(g, "1a2a3a4a");
		testFailure(g, "s");
	}

	private static void testSuccess(BNF grammar, String input) {
		Lexer l = new Lexer(input);
		RDParser test = new RDParser(grammar, l, EBNFParsedNodeFactory.INSTANCE);
		DefaultParsedNode root = test.parse();
		System.out.println(GraphViz.toVizDotLink(root));
		root = test.buildAst(root);
		System.out.println(GraphViz.toVizDotLink(root));

		assertEquals(SUCCESSFUL, root.getMatcher().state);

		DefaultParsedNode parsed = root.getChildren()[0];
		assertEquals(input.length() / 2, parsed.numChildren());

		int i = 0;
		for(DefaultParsedNode child : parsed.getChildren()) {
			assertEquals(input.substring(i, i + 2), child.getParsedString());
			assertEquals(2, child.numChildren());
			i += 2;
		}

		// test evaluate
		Object[] evaluated = (Object[]) parsed.evaluate();
		for(i = 0; i < evaluated.length; i++)
			assertEquals(input.substring(2 * i, 2 * i + 2), evaluated[i]);

		// test names
		for(DefaultParsedNode child : parsed.getChildren())
			assertEquals("seq", child.getName());
	}

	private static void testFailure(BNF grammar, String input) {
		Lexer l = new Lexer(input);
		RDParser parser = new RDParser(grammar, l, EBNFParsedNodeFactory.INSTANCE);
		DefaultParsedNode root = parser.parse();
		System.out.println(GraphViz.toVizDotLink(root));

		assertNotEquals(SUCCESSFUL, root.getMatcher().state);
	}
}
