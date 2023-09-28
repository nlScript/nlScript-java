package de.nls.core;

import de.nls.ParseException;
import de.nls.ebnf.EBNFCore;
import de.nls.ebnf.EBNFParsedNodeFactory;
import de.nls.ebnf.Rule;
import org.junit.jupiter.api.Test;

import static de.nls.core.ParsingState.SUCCESSFUL;
import static org.junit.jupiter.api.Assertions.*;

public class TestOptional {
	private static BNF makeGrammar() {
		EBNFCore grammar = new EBNFCore();
		Rule rule = grammar.optional("optional",
				grammar.sequence("seq",
						Terminal.DIGIT.withName(),
						Terminal.LETTER.withName()
				).withName("seq"));
		grammar.compile(rule.getTarget());
		return grammar.getBNF();
	}

	@Test
	public void test01() throws ParseException {
		testSuccess("");
	}

	@Test
	public void test02() throws ParseException {
		testSuccess("1a");
	}

	@Test
	public void test03() {
		testFailure("123");
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
		try {
			DefaultParsedNode pn = parser.parse();
			assertNotEquals(ParsingState.SUCCESSFUL, pn.getMatcher().state);
		} catch (ParseException ignored) {
		}
	}
}
