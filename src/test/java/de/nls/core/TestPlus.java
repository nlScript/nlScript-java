package de.nls.core;

import de.nls.ebnf.EBNFCore;
import de.nls.ebnf.EBNFParsedNodeFactory;
import de.nls.ebnf.Named;
import de.nls.ebnf.Rule;
import org.junit.jupiter.api.Test;

import static de.nls.core.ParsingState.SUCCESSFUL;
import static org.junit.jupiter.api.Assertions.*;

public class TestPlus {

	private static BNF makeGrammar() {
		EBNFCore grammar = new EBNFCore();
		Rule rule = grammar.plus("plus",
				Named.n("seq", grammar.sequence("seq",
						Named.n(Terminal.DIGIT),
						Named.n(Terminal.LETTER))));
		grammar.compile(rule.getTarget());
		return grammar.getBNF();
	}

	@Test
	public void test01() {
		testSuccess("1a2b3c");
	}

	@Test
	public void test02() {
		testSuccess("1a");
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
		RDParser parser = new RDParser(grammar, l, EBNFParsedNodeFactory.INSTANCE);
		DefaultParsedNode root = parser.parse();
		System.out.println(GraphViz.toVizDotLink(root));
		root = parser.buildAst(root);
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

	private static void testFailure(String input) {
		BNF grammar = makeGrammar();

		Lexer l = new Lexer(input);
		RDParser parser = new RDParser(grammar, l, EBNFParsedNodeFactory.INSTANCE);
		DefaultParsedNode root = parser.parse();
		System.out.println(GraphViz.toVizDotLink(root));

		assertNotEquals(SUCCESSFUL, root.getMatcher().state);
	}
}
