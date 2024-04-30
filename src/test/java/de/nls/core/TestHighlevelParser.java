package de.nls.core;

import de.nls.ParseException;
import de.nls.ParsedNode;
import de.nls.Parser;
import de.nls.ebnf.EBNF;
import de.nls.ebnf.EBNFCore;
import de.nls.ebnf.EBNFParsedNodeFactory;
import de.nls.ebnf.Join;
import de.nls.ebnf.Plus;
import de.nls.ebnf.Repeat;
import de.nls.ebnf.Rule;
import de.nls.ebnf.Star;
import de.nls.util.Range;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestHighlevelParser {

	private static Object evaluate(EBNF grammar, String input) throws ParseException {
		Lexer lexer = new Lexer(input);
		RDParser parser = new RDParser(grammar.getBNF(), lexer, EBNFParsedNodeFactory.INSTANCE);
		DefaultParsedNode p = parser.parse();
		System.out.println(GraphViz.toVizDotLink(p));

		return p.evaluate();
	}

	private static void checkFailed(BNF grammar, String input) {
		Lexer lexer = new Lexer(input);
		RDParser parser = new RDParser(grammar, lexer, EBNFParsedNodeFactory.INSTANCE);
		try {
			DefaultParsedNode pn = parser.parse();
			assertNotEquals(ParsingState.SUCCESSFUL, pn.getMatcher().state);
		} catch (ParseException ignored) {
		}
	}

	@Test
	public void testQuantifier() throws ParseException {
		System.out.println("Test Quantifier");
		Parser hlp = new Parser();
		EBNF grammar = hlp.getGrammar();
		grammar.compile(hlp.QUANTIFIER.getTarget());

		assertEquals(Range.OPTIONAL,  evaluate(grammar, "?"));
		assertEquals(Range.STAR,      evaluate(grammar, "*"));
		assertEquals(Range.PLUS,      evaluate(grammar, "+"));
		assertEquals(new Range(1, 5), evaluate(grammar, "1-5"));
		assertEquals(new Range(3),    evaluate(grammar, "3"));
	}

	@Test
	public void testIdentifier() throws ParseException {
		System.out.println("Test Identifier");
		Parser hlp = new Parser();
		EBNF grammar = hlp.getGrammar();
		grammar.compile(hlp.IDENTIFIER.getTarget());
		System.out.println(grammar);

		String[] positives = new String[]{"bla", "_1-lkj", "A", "_"};
		String[] negatives = new String[]{"-abc", "abc-"};

		for (String test : positives) {
			System.out.println("Testing " + test);
			assertEquals(test, evaluate(grammar, test));
		}
		for (String test : negatives) {
			System.out.println("Testing " + test);
			checkFailed(grammar.getBNF(), test);
		}
	}

	private Object evaluateHighlevelParser(Parser hlp, String input) throws ParseException {
		Lexer lexer = new Lexer(input);
		RDParser parser = new RDParser(hlp.getGrammar().getBNF(), lexer, EBNFParsedNodeFactory.INSTANCE);
		DefaultParsedNode p = parser.parse();
		if(p.getMatcher().state != ParsingState.SUCCESSFUL)
			throw new RuntimeException("Parsing failed");
		return p.evaluate();
	}

	@Test
	public void testList() throws ParseException {
		System.out.println("Test List");
		Parser hlp = new Parser();
		EBNF grammar = hlp.getGrammar();
		grammar.compile(hlp.LIST.getTarget());

		String test = "list<int>";
		Join list = (Join) evaluateHighlevelParser(hlp, test);

		// now parse and evaluate the generated grammar:
		EBNFCore tgt = hlp.getTargetGrammar();
		tgt.compile(list.getTarget());
		RDParser rdParser = new RDParser(tgt.getBNF(), new Lexer("1, 2, 3"), EBNFParsedNodeFactory.INSTANCE);
		DefaultParsedNode pn = rdParser.parse();
		assertEquals(ParsingState.SUCCESSFUL, pn.getMatcher().state);
		Object[] result = (Object[]) pn.evaluate();

		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
		assertEquals(3, result[2]);
	}

	@Test
	public void testTuple() throws ParseException {
		System.out.println("Test Tuple");
		Parser hlp = new Parser();
		EBNF grammar = hlp.getGrammar();
		grammar.compile(hlp.TUPLE.getTarget());

		String test = "tuple<int,x, y>";
		NonTerminal tuple = (NonTerminal) evaluateHighlevelParser(hlp, test);

		// now parse and evaluate the generated grammar:
		EBNFCore tgt = hlp.getTargetGrammar();
		tgt.compile(tuple);
		RDParser rdParser = new RDParser(tgt.getBNF(), new Lexer("(1, 2)"), EBNFParsedNodeFactory.INSTANCE);

		DefaultParsedNode pn = rdParser.parse();
		System.out.println(GraphViz.toVizDotLink(pn));

		assertEquals(ParsingState.SUCCESSFUL, pn.getMatcher().state);
		Object[] result = (Object[]) pn.evaluate();

		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
	}

	@Test
	public void testCharacterClass() throws ParseException {
		System.out.println("Test Character Class");
		Parser hlp = new Parser();
		EBNF grammar = hlp.getGrammar();
		grammar.compile(hlp.CHARACTER_CLASS.getTarget());

		Terminal.CharacterClass nt = (Terminal.CharacterClass) evaluate(grammar, "[a-zA-Z]");
		assertEquals(Terminal.characterClass("[a-zA-Z]"), nt);
	}

	@Test
	public void testType() throws ParseException {
		Parser hlp = new Parser();
		EBNF grammar = hlp.getGrammar();
		grammar.compile(hlp.TYPE.getTarget());

		// test tuple
		String test = "tuple<int,x,y,z>";
		NonTerminal tuple = (NonTerminal) evaluateHighlevelParser(hlp, test);

		// now parse and evaluate the generated grammar:
		EBNFCore tgt = hlp.getTargetGrammar();
		tgt.compile(tuple);
		RDParser rdParser = new RDParser(tgt.getBNF(), new Lexer("(1, 2, 3)"), EBNFParsedNodeFactory.INSTANCE);
		DefaultParsedNode pn = rdParser.parse();
		assertEquals(ParsingState.SUCCESSFUL, pn.getMatcher().state);
		System.out.println(GraphViz.toVizDotLink(pn));
		Object[] result = (Object[]) pn.evaluate();

		assertEquals(1, result[0]);
		assertEquals(2, result[1]);


		// test list
		hlp = new Parser();
		grammar = hlp.getGrammar();
		grammar.compile(hlp.TYPE.getTarget());
		test = "list<int>";
		Join list = (Join) evaluateHighlevelParser(hlp, test);

		// now parse and evaluate the generated grammar:
		tgt = hlp.getTargetGrammar();
		tgt.compile(list.getTarget());
		rdParser = new RDParser(tgt.getBNF(), new Lexer("1, 2, 3"), EBNFParsedNodeFactory.INSTANCE);
		pn = rdParser.parse();
		System.out.println(GraphViz.toVizDotLink(pn));
		assertEquals(ParsingState.SUCCESSFUL, pn.getMatcher().state);
		result = (Object[]) pn.evaluate();

		assertEquals(1, result[0]);
		assertEquals(2, result[1]);
		assertEquals(3, result[2]);

		// test identifier
		hlp = new Parser();
		grammar = hlp.getGrammar();
		grammar.compile(hlp.TYPE.getTarget());
		test = "int";
		NonTerminal identifier = (NonTerminal) evaluateHighlevelParser(hlp, test);

		// now parse and evaluate the generated grammar:
		tgt = hlp.getTargetGrammar();
		tgt.compile(identifier);
		rdParser = new RDParser(tgt.getBNF(), new Lexer("3"), EBNFParsedNodeFactory.INSTANCE);
		pn = rdParser.parse();
		System.out.println(GraphViz.toVizDotLink(pn));
		assertEquals(ParsingState.SUCCESSFUL, pn.getMatcher().state);
		assertEquals(3, pn.evaluate());
	}

	@Test
	public void testVariable() throws ParseException {
		System.out.println("Test Variable");
		Parser hlp = new Parser();
		EBNF grammar = hlp.getGrammar();
		grammar.compile(hlp.VARIABLE.getTarget());

		String test = "{bla:int:3-5}";
		Named<NonTerminal> evaluatedNonTerminal = (Named<NonTerminal>) evaluateHighlevelParser(hlp, test);
		assertEquals("bla", evaluatedNonTerminal.getName());
		Rule rule = hlp.getTargetGrammar().getRules(evaluatedNonTerminal.get()).get(0);
		assertEquals(Repeat.class, rule.getClass());
		Repeat repeat = (Repeat) rule;
		assertEquals(3, repeat.getFrom());
		assertEquals(5, repeat.getTo());
		assertEquals(EBNF.INTEGER_NAME, repeat.getEntry().getSymbol());

		Named<Terminal> evaluatedTerminal;
		test = "{blubb:digit}";
		evaluatedNonTerminal = (Named<NonTerminal>) evaluateHighlevelParser(hlp, test);
		assertEquals("blubb", evaluatedNonTerminal.getName());
		assertEquals(Terminal.DIGIT.getSymbol(), evaluatedNonTerminal.getSymbol().getSymbol());

		test = "{blubb:int:*}";
		evaluatedNonTerminal = (Named<NonTerminal>) evaluateHighlevelParser(hlp, test);
		assertEquals("blubb", evaluatedNonTerminal.getName());
		rule = hlp.getTargetGrammar().getRules(evaluatedNonTerminal.get()).get(0);
		assertEquals(Star.class, rule.getClass());
		Star star = (Star) rule;
		assertEquals(EBNF.INTEGER_NAME, star.getEntry().getSymbol());

		test = "{blubb:[A-Z]:+}";
		evaluatedNonTerminal = (Named<NonTerminal>) evaluateHighlevelParser(hlp, test);
		assertEquals("blubb", evaluatedNonTerminal.getName());
		rule = hlp.getTargetGrammar().getRules(evaluatedNonTerminal.get()).get(0);
		assertEquals(Plus.class, rule.getClass());
		Plus plus = (Plus) rule;
		Symbol chclass = plus.getEntry();
		assertEquals("[A-Z]", chclass.getSymbol());

		test = "{blubb , alkjad asd 4. <>l}";
		evaluatedTerminal = (Named<Terminal>) evaluateHighlevelParser(hlp, test);
		assertEquals("blubb , alkjad asd 4. <>l", evaluatedTerminal.getSymbol().getSymbol());
		assertEquals("blubb , alkjad asd 4. <>l", evaluatedTerminal.getName());

		test = "{heinz}";
		evaluatedTerminal = (Named<Terminal>) evaluateHighlevelParser(hlp, test);
		assertEquals("heinz", evaluatedTerminal.getSymbol().getSymbol());
		assertEquals("heinz", evaluatedTerminal.getName());

		test = "{heinz:+}";
		evaluatedNonTerminal = (Named<NonTerminal>) evaluateHighlevelParser(hlp, test);
		assertEquals("heinz", evaluatedNonTerminal.getName());
		rule = hlp.getTargetGrammar().getRules(evaluatedNonTerminal.get()).get(0);
		assertEquals(Plus.class, rule.getClass());
		plus = (Plus) rule;
		assertEquals("heinz", plus.getEntry().getSymbol());

		test = "{heinz:3-5}";
		evaluatedNonTerminal = (Named<NonTerminal>) evaluateHighlevelParser(hlp, test);
		assertEquals("heinz", evaluatedNonTerminal.getName());
		rule = hlp.getTargetGrammar().getRules(evaluatedNonTerminal.get()).get(0);
		assertEquals(Repeat.class, rule.getClass());
		repeat = (Repeat) rule;
		assertEquals(3, repeat.getFrom());
		assertEquals(5, repeat.getTo());
		assertEquals("heinz", repeat.getEntry().getSymbol());

		test = "{, }";
		evaluatedTerminal = (Named<Terminal>) evaluateHighlevelParser(hlp, test);
		assertEquals(", ", evaluatedTerminal.getSymbol().getSymbol());
		assertEquals(", ", evaluatedTerminal.getName());

		test = "{,\n }";
		evaluatedTerminal = (Named<Terminal>) evaluateHighlevelParser(hlp, test);
		assertEquals(",\n ", evaluatedTerminal.getSymbol().getSymbol());
		assertEquals(",\n ", evaluatedTerminal.getName());
	}

	@Test
	public void testNoVariable() throws ParseException {
		System.out.println("Test NoVariable");
		Parser hlp = new Parser();
		EBNF grammar = hlp.getGrammar();
		grammar.compile(hlp.NO_VARIABLE.getTarget());

		String test = "lk345}.-";
		Named<Terminal> evaluatedTerminal = (Named<Terminal>) evaluateHighlevelParser(hlp, test);
		assertEquals(Terminal.Literal.class, evaluatedTerminal.get().getClass());
		assertEquals(test, evaluatedTerminal.getSymbol().getSymbol());
		assertEquals(Named.UNNAMED, evaluatedTerminal.getName());

		test = "--1'x}";
		evaluatedTerminal = (Named<Terminal>) evaluateHighlevelParser(hlp, test);
		assertEquals(Terminal.Literal.class, evaluatedTerminal.get().getClass());
		assertEquals(test, evaluatedTerminal.getSymbol().getSymbol());
		assertEquals(Named.UNNAMED, evaluatedTerminal.getName());

		test = ".";
		evaluatedTerminal = (Named<Terminal>) evaluateHighlevelParser(hlp, test);
		assertEquals(Terminal.Literal.class, evaluatedTerminal.get().getClass());
		assertEquals(test, evaluatedTerminal.getSymbol().getSymbol());
		assertEquals(Named.UNNAMED, evaluatedTerminal.getName());

		String testToFail = "lj{l";
		assertThrows(ParseException.class, () -> evaluateHighlevelParser(hlp, testToFail));

	}

	@Test
	public void testExpression() throws ParseException {
		System.out.println("Test Expression");
		Parser hlp = new Parser();
		EBNF grammar = hlp.getGrammar();
		grammar.compile(hlp.EXPRESSION.getTarget());

		String test = "Today, let's wait for {time:int} minutes.";
		Named<?>[] rhs = (Named<?>[]) evaluateHighlevelParser(hlp, test);
		EBNFCore tgt = hlp.getTargetGrammar();
		Rule myType = tgt.sequence("mytype", rhs);

		// now parse and evaluate the generated grammar:
		tgt.compile(myType.getTarget());
		RDParser rdParser = new RDParser(tgt.getBNF(), new Lexer("Today, let's wait for 5 minutes."), EBNFParsedNodeFactory.INSTANCE);
		DefaultParsedNode pn = rdParser.parse();
		assertEquals(ParsingState.SUCCESSFUL, pn.getMatcher().state);
		System.out.println(GraphViz.toVizDotLink(pn));
	}

	@Test
	public void testDefineType() throws ParseException {
		System.out.println("Test define type");
		Parser hlp = new Parser();
		hlp.defineType("percentage", "{p:int} %", pn -> pn.evaluate("p"));

		hlp.defineSentence("Now it is only {p:percentage}.", pn -> {
			int percentage = (int) pn.evaluate("p");
			assertEquals(5, percentage);
			System.out.println(percentage + " % left.");
			return null;
		});
		hlp.defineSentence("There is still {p:percentage} left.", pn -> {
			int percentage = (int) pn.evaluate("p");
			assertEquals(38, percentage);
			System.out.println(percentage + " % left.");
			return null;
		});

		ParsedNode pn = hlp.parse(
				"There is still 38 % left.\n" +
				"Now it is only 5 %.", null);

		assertEquals(pn.getMatcher().state, ParsingState.SUCCESSFUL);
		pn.evaluate();
	}
}
