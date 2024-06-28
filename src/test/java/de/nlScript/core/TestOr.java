package de.nlScript.core;

import de.nlScript.ParseException;
import de.nlScript.ebnf.EBNFCore;
import de.nlScript.ebnf.EBNFParsedNodeFactory;
import de.nlScript.ebnf.Rule;
import org.junit.jupiter.api.Test;

import static de.nlScript.core.ParsingState.SUCCESSFUL;
import static org.junit.jupiter.api.Assertions.*;

public class TestOr {

	private static BNF makeGrammar() {
		EBNFCore grammar = new EBNFCore();
		Rule rule = grammar.or("or",
				grammar.sequence("seq1",
					Terminal.literal("y").withName(),
					Terminal.DIGIT.withName()
				).withName("seq"),
				grammar.sequence("seq2",
					Terminal.literal("n").withName(),
					Terminal.DIGIT.withName()
				).withName("seq")
		);
		grammar.compile(rule.getTarget());
		return grammar.getBNF();
	}

	@Test
	public void test01() throws ParseException {
		testSuccess("y1");
	}

	@Test
	public void test02() throws ParseException {
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

	private static void testSuccess(String input) throws ParseException {
		BNF grammar = makeGrammar();

		Lexer l = new Lexer(input);
		RDParser test = new RDParser(grammar, l, EBNFParsedNodeFactory.INSTANCE);
		DefaultParsedNode root = test.parse();
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
		try {
			DefaultParsedNode pn = parser.parse();
			assertNotEquals(ParsingState.SUCCESSFUL, pn.getMatcher().state);
		} catch (ParseException ignored) {
		}
	}
}
