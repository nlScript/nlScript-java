package de.nls.core;

import de.nls.ebnf.EBNFCore;
import de.nls.ebnf.EBNFParsedNodeFactory;
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
				Named.n("seq", grammar.sequence("seq1",
					Named.n(Terminal.literal("y")),
					Named.n(Terminal.DIGIT))),
				Named.n("seq", grammar.sequence("seq2",
					Named.n(Terminal.literal("n")),
					Named.n(Terminal.DIGIT))));
		grammar.setWhatToMatch(rule.getTarget());
		return grammar.createBNF();
	}

	@Test
	public void test01() {
		testSuccess("y1");
	}

	@Test
	public void test02() {
		testSuccess("n3");
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
		RDParser test = new RDParser(grammar, l, EBNFParsedNodeFactory.INSTANCE);
		DefaultParsedNode root = test.parse();
		System.out.println(GraphViz.toVizDotLink(root));
		root = test.buildAst(root);
		System.out.println(GraphViz.toVizDotLink(root));

		assertEquals(SUCCESSFUL, root.getMatcher().state);

		DefaultParsedNode parsed = root.getChildren()[0];
		assertEquals(1, parsed.numChildren());

		DefaultParsedNode child = parsed.getChild(0);
		assertEquals(input, child.getParsedString());
		assertEquals(2, child.numChildren());

		// test evaluate
		Object evaluated = parsed.evaluate();
		assertEquals(input, evaluated);

		// test names
		assertEquals("seq", child.getName());
	}

	private static void testFailure(String input) {
		BNF grammar = makeGrammar();

		Lexer l = new Lexer(input);
		RDParser parser = new RDParser(grammar, l, EBNFParsedNodeFactory.INSTANCE);
		DefaultParsedNode root = parser.parse();
		System.out.println(GraphViz.toVizDotLink(root));

		assertNotEquals(SUCCESSFUL, root.getMatcher().state);
	}
}
