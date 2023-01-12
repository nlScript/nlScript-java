package de.nls.core;

import de.nls.ParsedNode;
import de.nls.ebnf.EBNFCore;
import de.nls.ebnf.Named;
import de.nls.ebnf.Rule;
import de.nls.util.Range;
import org.junit.jupiter.api.Test;

import static de.nls.core.ParsingState.SUCCESSFUL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class TestJoin {

	@Test
	public void testKeepDelimiters() {
		EBNFCore grammar = new EBNFCore();
		Rule rule = grammar.join("join",
				Named.n(Terminal.DIGIT),
				Terminal.literal("("),
				Terminal.literal(")"),
				Terminal.literal(","),
				false, // onlyKeepEntries
				"ha", "ho", "hu");
		grammar.setWhatToMatch(rule.getTarget());

		String input = "(1,3,4)";
		Lexer l = new Lexer(input);
		RDParser test = new RDParser(grammar.createBNF(), l);
		ParsedNode root = test.parse();
		System.out.println(GraphViz.toVizDotLink(root));
		root = test.buildAst(root);
		System.out.println(GraphViz.toVizDotLink(root));

		assertEquals(SUCCESSFUL, root.getMatcher().state);

		ParsedNode parsedJoinNode = root.getChild(0);
		assertEquals(7, parsedJoinNode.numChildren());

		// test names
		assertEquals("open",      parsedJoinNode.getChild(0).getName());
		assertEquals("ha",        parsedJoinNode.getChild(1).getName());
		assertEquals("delimiter", parsedJoinNode.getChild(2).getName());
		assertEquals("ho",        parsedJoinNode.getChild(3).getName());
		assertEquals("delimiter", parsedJoinNode.getChild(4).getName());
		assertEquals("hu",        parsedJoinNode.getChild(5).getName());
		assertEquals("close",     parsedJoinNode.getChild(6).getName());
	}

	@Test
	public void test() {
		boolean[] withOpenClose = new boolean[] { true, true, false, false };
		boolean[] withDelimiter = new boolean[] { true, false, true, false };
		String[][] inputs = new String[][] {
				{ // with open/close, with delimiter
						"",
						"()",
						"(1)",
						"(1,2)",
						"(1,2,3)",
						"1,2,3",
						"s"
				},
				{ // with open/close, without delimiter
						"",
						"()",
						"(1)",
						"(12)",
						"(123)",
						"123",
						"s"
				},
				{ // without open/close, with delimiter
						"()",
						"",
						"1",
						"1,2",
						"1,2,3",
						"(1,2,3)",
						"s"
				},
				{ // without open/close, without delimiter
						"()",
						"",
						"1",
						"12",
						"123",
						"(123)",
						"s"
				},
		};

		for(int i = 0; i < 3; i++) {
			BNF grammar = makeGrammar(withOpenClose[i], withDelimiter[i], Range.PLUS);
			testFailure(grammar, inputs[i][0]);
			testFailure(grammar, inputs[i][1]);
			testSuccess(grammar, inputs[i][2], "1");
			testSuccess(grammar, inputs[i][3], "1", "2");
			testSuccess(grammar, inputs[i][4], "1", "2", "3");
			testFailure(grammar, inputs[i][5]);
			testFailure(grammar, inputs[i][6]);

			grammar = makeGrammar(withOpenClose[i], withDelimiter[i], Range.STAR);
			testFailure(grammar, inputs[i][0]);
			testSuccess(grammar, inputs[i][1]);
			testSuccess(grammar, inputs[i][2], "1");
			testSuccess(grammar, inputs[i][3], "1", "2");
			testSuccess(grammar, inputs[i][4], "1", "2", "3");
			testFailure(grammar, inputs[i][5]);
			testFailure(grammar, inputs[i][6]);

			grammar = makeGrammar(withOpenClose[i], withDelimiter[i], Range.OPTIONAL);
			testFailure(grammar, inputs[i][0]);
			testSuccess(grammar, inputs[i][1]);
			testSuccess(grammar, inputs[i][2], "1");
			testFailure(grammar, inputs[i][3]);
			testFailure(grammar, inputs[i][4]);
			testFailure(grammar, inputs[i][5]);
			testFailure(grammar, inputs[i][6]);

			grammar = makeGrammar(withOpenClose[i], withDelimiter[i], new Range(0, 0));
			testFailure(grammar, inputs[i][0]);
			testSuccess(grammar, inputs[i][1]);
			testFailure(grammar, inputs[i][2]);
			testFailure(grammar, inputs[i][3]);
			testFailure(grammar, inputs[i][4]);
			testFailure(grammar, inputs[i][5]);
			testFailure(grammar, inputs[i][6]);

			grammar = makeGrammar(withOpenClose[i], withDelimiter[i], new Range(1, 1));
			testFailure(grammar, inputs[i][0]);
			testFailure(grammar, inputs[i][1]);
			testSuccess(grammar, inputs[i][2], "1");
			testFailure(grammar, inputs[i][3]);
			testFailure(grammar, inputs[i][4]);
			testFailure(grammar, inputs[i][5]);
			testFailure(grammar, inputs[i][6]);

			grammar = makeGrammar(withOpenClose[i], withDelimiter[i], new Range(0, 2));
			testFailure(grammar, inputs[i][0]);
			testSuccess(grammar, inputs[i][1]);
			testSuccess(grammar, inputs[i][2], "1");
			testSuccess(grammar, inputs[i][3], "1", "2");
			testFailure(grammar, inputs[i][4]);
			testFailure(grammar, inputs[i][5]);
			testFailure(grammar, inputs[i][6]);

			grammar = makeGrammar(withOpenClose[i], withDelimiter[i], new Range(1, 2));
			testFailure(grammar, inputs[i][0]);
			testFailure(grammar, inputs[i][1]);
			testSuccess(grammar, inputs[i][2], "1");
			testSuccess(grammar, inputs[i][3], "1", "2");
			testFailure(grammar, inputs[i][4]);
			testFailure(grammar, inputs[i][5]);
			testFailure(grammar, inputs[i][6]);
		}
	}

	private static BNF makeGrammar(boolean withOpenAndClose, boolean withDelimiter, Range range) {
		EBNFCore grammar = new EBNFCore();
		Rule rule = grammar.join("join",
				Named.n(Terminal.DIGIT),
				withOpenAndClose ? Terminal.literal("(") : null,
				withOpenAndClose ? Terminal.literal(")") : null,
				withDelimiter    ? Terminal.literal(",") : null,
				range);
		grammar.setWhatToMatch(rule.getTarget());
		return grammar.createBNF();
	}

	private static void testSuccess(BNF grammar, String input, String... result) {
		Lexer l = new Lexer(input);
		RDParser test = new RDParser(grammar, l);
		ParsedNode root = test.parse();
		System.out.println(GraphViz.toVizDotLink(root));
		root = test.buildAst(root);
		System.out.println(GraphViz.toVizDotLink(root));

		assertEquals(SUCCESSFUL, root.getMatcher().state);

		ParsedNode parsed = root.getChildren()[0];
		assertEquals(result.length, parsed.numChildren());
		assertEquals(input, parsed.getParsedString());

		int i = 0;
		for(ParsedNode child : parsed.getChildren()) {
			assertEquals(result[i++], child.getParsedString());
			assertEquals(0, child.numChildren());
		}

		// test evaluate
		Object[] evaluated = (Object[]) parsed.evaluate();
		for(i = 0; i < evaluated.length; i++)
			assertEquals(result[i], evaluated[i]);

		// test names
		for(ParsedNode child : parsed.getChildren())
			assertEquals(Terminal.DIGIT.getSymbol(), child.getName());
	}

	private static void testFailure(BNF grammar, String input) {
		Lexer l = new Lexer(input);
		RDParser parser = new RDParser(grammar, l);
		ParsedNode root = parser.parse();
		System.out.println(GraphViz.toVizDotLink(root));

		assertNotEquals(SUCCESSFUL, root.getMatcher().state);
	}
}
