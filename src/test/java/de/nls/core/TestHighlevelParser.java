package de.nls.core;

import de.nls.ParsedNode;
import de.nls.Parser;
import de.nls.ebnf.EBNF;
import de.nls.ebnf.EBNFCore;
import de.nls.ebnf.EBNFParsedNodeFactory;
import de.nls.ebnf.Named;
import de.nls.ebnf.Plus;
import de.nls.ebnf.Repeat;
import de.nls.ebnf.Rule;
import de.nls.ebnf.Star;
import de.nls.util.Range;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestHighlevelParser {

	private static Object evaluate(EBNF grammar, String input) {
		Lexer lexer = new Lexer(input);
		RDParser parser = new RDParser(grammar.createBNF(), lexer, EBNFParsedNodeFactory.INSTANCE);
		DefaultParsedNode p = parser.parse();
		System.out.println(GraphViz.toVizDotLink(p));
		p = parser.buildAst(p);
		System.out.println(GraphViz.toVizDotLink(p));

		return p.evaluate();
	}

	private static void checkFailed(BNF grammar, String input) {
		Lexer lexer = new Lexer(input);
		RDParser parser = new RDParser(grammar, lexer, EBNFParsedNodeFactory.INSTANCE);
		DefaultParsedNode p = parser.parse();
		System.out.println(GraphViz.toVizDotLink(p));
		if (p.getMatcher().state == ParsingState.SUCCESSFUL)
			throw new RuntimeException();
	}

	@Test
	public void testQuantifier() {
		System.out.println("Test Quantifier");
		Parser hlp = new Parser();
		EBNF grammar = hlp.getGrammar();
		grammar.setWhatToMatch(hlp.QUANTIFIER.getTarget());

		if (!evaluate(grammar, "?").equals(Range.OPTIONAL))
			throw new RuntimeException();

		if (!evaluate(grammar, "*").equals(Range.STAR))
			throw new RuntimeException();

		if (!evaluate(grammar, "+").equals(Range.PLUS))
			throw new RuntimeException();

		if (!evaluate(grammar, "1-5").equals(new Range(1, 5)))
			throw new RuntimeException();

		if (!evaluate(grammar, "3").equals(new Range(3)))
			throw new RuntimeException();
	}

	@Test
	public void testIdentifier() {
		System.out.println("Test Identifier");
		Parser hlp = new Parser();
		EBNF grammar = hlp.getGrammar();
		grammar.setWhatToMatch(hlp.IDENTIFIER.getTarget());
		System.out.println(grammar);

		String[] positives = new String[]{"bla", "_1-lkj", "A", "_"};
		String[] negatives = new String[]{"-abc", "abc-"};

		for (String test : positives) {
			System.out.println("Testing " + test);
			if (!evaluate(grammar, test).equals(test))
				throw new RuntimeException();
		}
		for (String test : negatives) {
			System.out.println("Testing " + test);
			checkFailed(grammar.createBNF(), test);
		}
	}

	private Object evaluateHighlevelParser(Parser hlp, String input) {
		Lexer lexer = new Lexer(input);
		RDParser parser = new RDParser(hlp.getGrammar().createBNF(), lexer, EBNFParsedNodeFactory.INSTANCE);
		DefaultParsedNode p = parser.parse();
		if(p.getMatcher().state != ParsingState.SUCCESSFUL)
			throw new RuntimeException("Parsing failed");
		p = parser.buildAst(p);
		return p.evaluate();
	}

	@Test
	public void testList() {
		System.out.println("Test List");
		Parser hlp = new Parser();
		EBNF grammar = hlp.getGrammar();
		grammar.setWhatToMatch(hlp.LIST.getTarget());

		String test = "list<int>";
		NonTerminal list = (NonTerminal) evaluateHighlevelParser(hlp, test);

		// now parse and evaluate the generated grammar:
		EBNFCore tgt = hlp.getTargetGrammar();
		tgt.setWhatToMatch(list);
		RDParser rdParser = new RDParser(tgt.createBNF(), new Lexer("1, 2, 3"), EBNFParsedNodeFactory.INSTANCE);
		DefaultParsedNode pn = rdParser.buildAst(rdParser.parse());
		if(pn.getMatcher().state != ParsingState.SUCCESSFUL)
			throw new RuntimeException();
		Object[] result = (Object[]) pn.evaluate();

		if ((int) result[0] != 1 || (int) result[1] != 2 || (int) result[2] != 3)
			throw new RuntimeException();
	}

	@Test
	public void testTuple() {
		System.out.println("Test Tuple");
		Parser hlp = new Parser();
		EBNF grammar = hlp.getGrammar();
		grammar.setWhatToMatch(hlp.TUPLE.getTarget());

		String test = "tuple<int,x, y>";
		NonTerminal tuple = (NonTerminal) evaluateHighlevelParser(hlp, test);

		// now parse and evaluate the generated grammar:
		EBNFCore tgt = hlp.getTargetGrammar();
		tgt.setWhatToMatch(tuple);
		RDParser rdParser = new RDParser(tgt.createBNF(), new Lexer("(1, 2)"), EBNFParsedNodeFactory.INSTANCE);

		DefaultParsedNode pn = rdParser.parse();
		System.out.println(GraphViz.toVizDotLink(pn));

		if(pn.getMatcher().state != ParsingState.SUCCESSFUL)
			throw new RuntimeException();
		pn = rdParser.buildAst(pn);
		System.out.println(GraphViz.toVizDotLink(pn));
		Object[] result = (Object[]) pn.evaluate();

		if ((int) result[0] != 1 || (int) result[1] != 2)
			throw new RuntimeException();
	}

	@Test
	public void testCharacterClass() {
		System.out.println("Test Character Class");
		Parser hlp = new Parser();
		EBNF grammar = hlp.getGrammar();
		grammar.setWhatToMatch(hlp.CHARACTER_CLASS.getTarget());

		Terminal.CharacterClass cc = (Terminal.CharacterClass) evaluate(grammar, "[a-zA-Z]");
		if(!cc.equals(Terminal.characterClass("[a-zA-Z]")))
			throw new RuntimeException();
	}

	@Test
	public void testType() {
		Parser hlp = new Parser();
		EBNF grammar = hlp.getGrammar();
		grammar.setWhatToMatch(hlp.TYPE.getTarget());

		// test tuple
		String test = "tuple<int,x,y,z>";
		NonTerminal tuple = (NonTerminal) evaluateHighlevelParser(hlp, test);

		// now parse and evaluate the generated grammar:
		EBNFCore tgt = hlp.getTargetGrammar();
		tgt.setWhatToMatch(tuple);
		RDParser rdParser = new RDParser(tgt.createBNF(), new Lexer("(1, 2, 3)"), EBNFParsedNodeFactory.INSTANCE);
		DefaultParsedNode pn = rdParser.buildAst(rdParser.parse());
		if(pn.getMatcher().state != ParsingState.SUCCESSFUL)
			throw new RuntimeException();
		System.out.println(GraphViz.toVizDotLink(pn));
		Object[] result = (Object[]) pn.evaluate();

		if ((int) result[0] != 1 || (int) result[1] != 2)
			throw new RuntimeException();


		// test list
		test = "list<int>";
		NonTerminal list = (NonTerminal) evaluateHighlevelParser(hlp, test);

		// now parse and evaluate the generated grammar:
		tgt = hlp.getTargetGrammar();
		tgt.setWhatToMatch(list);
		rdParser = new RDParser(tgt.createBNF(), new Lexer("1, 2, 3"), EBNFParsedNodeFactory.INSTANCE);
		pn = rdParser.buildAst(rdParser.parse());
		System.out.println(GraphViz.toVizDotLink(pn));
		if(pn.getMatcher().state != ParsingState.SUCCESSFUL)
			throw new RuntimeException();
		result = (Object[]) pn.evaluate();

		if ((int) result[0] != 1 || (int) result[1] != 2 || (int) result[2] != 3)
			throw new RuntimeException();

		// test identifier
		test = "int";
		NonTerminal identifier = (NonTerminal) evaluateHighlevelParser(hlp, test);

		// now parse and evaluate the generated grammar:
		tgt = hlp.getTargetGrammar();
		tgt.setWhatToMatch(identifier);
		rdParser = new RDParser(tgt.createBNF(), new Lexer("3"), EBNFParsedNodeFactory.INSTANCE);
		pn = rdParser.buildAst(rdParser.parse());
		System.out.println(GraphViz.toVizDotLink(pn));
		if(pn.getMatcher().state != ParsingState.SUCCESSFUL)
			throw new RuntimeException();

		if (((int) pn.evaluate()) != 3)
			throw new RuntimeException();
	}

	@Test
	public void testVariable() {
		System.out.println("Test Variable");
		Parser hlp = new Parser();
		EBNF grammar = hlp.getGrammar();
		grammar.setWhatToMatch(hlp.VARIABLE.getTarget());

		String test = "{bla:int:3-5}";
		Named.NamedNonTerminal evaluatedNonTerminal = (Named.NamedNonTerminal) evaluateHighlevelParser(hlp, test);
		assertEquals("bla", evaluatedNonTerminal.getName());
		Rule rule = hlp.getTargetGrammar().getRules(evaluatedNonTerminal.getNonTerminal()).get(0);
		assertEquals(Repeat.class, rule.getClass());
		Repeat repeat = (Repeat) rule;
		assertEquals(3, repeat.getFrom());
		assertEquals(5, repeat.getTo());
		assertEquals(EBNF.INTEGER_NAME, repeat.getEntry().getSymbol());

		test = "{blubb:digit}";
		Named.NamedTerminal evaluatedTerminal = (Named.NamedTerminal) evaluateHighlevelParser(hlp, test);
		assertEquals("blubb", evaluatedTerminal.getName());
		assertEquals(Terminal.DIGIT, evaluatedTerminal.getSymbol());

		test = "{blubb:int:*}";
		evaluatedNonTerminal = (Named.NamedNonTerminal) evaluateHighlevelParser(hlp, test);
		assertEquals("blubb", evaluatedNonTerminal.getName());
		rule = hlp.getTargetGrammar().getRules(evaluatedNonTerminal.getNonTerminal()).get(0);
		assertEquals(Star.class, rule.getClass());
		Star star = (Star) rule;
		assertEquals(EBNF.INTEGER_NAME, star.getEntry().getSymbol());

		test = "{blubb:[A-Z]:+}";
		evaluatedNonTerminal = (Named.NamedNonTerminal) evaluateHighlevelParser(hlp, test);
		assertEquals("blubb", evaluatedNonTerminal.getName());
		rule = hlp.getTargetGrammar().getRules(evaluatedNonTerminal.getNonTerminal()).get(0);
		assertEquals(Plus.class, rule.getClass());
		Plus plus = (Plus) rule;
		assertEquals("[A-Z]", plus.getEntry().getSymbol());

		test = "{blubb , alkjad asd 4. <>l}";
		evaluatedTerminal = (Named.NamedTerminal) evaluateHighlevelParser(hlp, test);
		assertEquals("blubb , alkjad asd 4. <>l", evaluatedTerminal.getSymbol().getSymbol());
		assertEquals("blubb , alkjad asd 4. <>l", evaluatedTerminal.getName());

		test = "{heinz}";
		evaluatedTerminal = (Named.NamedTerminal) evaluateHighlevelParser(hlp, test);
		assertEquals("heinz", evaluatedTerminal.getSymbol().getSymbol());
		assertEquals("heinz", evaluatedTerminal.getName());

		test = "{heinz:+}";
		evaluatedNonTerminal = (Named.NamedNonTerminal) evaluateHighlevelParser(hlp, test);
		assertEquals("heinz", evaluatedNonTerminal.getName());
		rule = hlp.getTargetGrammar().getRules(evaluatedNonTerminal.getNonTerminal()).get(0);
		assertEquals(Plus.class, rule.getClass());
		plus = (Plus) rule;
		assertEquals("heinz", plus.getEntry().getSymbol());

		test = "{heinz:3-5}";
		evaluatedNonTerminal = (Named.NamedNonTerminal) evaluateHighlevelParser(hlp, test);
		assertEquals("heinz", evaluatedNonTerminal.getName());
		rule = hlp.getTargetGrammar().getRules(evaluatedNonTerminal.getNonTerminal()).get(0);
		assertEquals(Repeat.class, rule.getClass());
		repeat = (Repeat) rule;
		assertEquals(3, repeat.getFrom());
		assertEquals(5, repeat.getTo());
		assertEquals("heinz", repeat.getEntry().getSymbol());

		test = "{, }";
		evaluatedTerminal = (Named.NamedTerminal) evaluateHighlevelParser(hlp, test);
		assertEquals(", ", evaluatedTerminal.getSymbol().getSymbol());
		assertEquals(", ", evaluatedTerminal.getName());

		test = "{,\n }";
		evaluatedTerminal = (Named.NamedTerminal) evaluateHighlevelParser(hlp, test);
		assertEquals(",\n ", evaluatedTerminal.getSymbol().getSymbol());
		assertEquals(",\n ", evaluatedTerminal.getName());
	}

	@Test
	public void testNoVariable() {
		System.out.println("Test NoVariable");
		Parser hlp = new Parser();
		EBNF grammar = hlp.getGrammar();
		grammar.setWhatToMatch(hlp.NO_VARIABLE.getTarget());

		String test = "lk345}.-";
		Named.NamedTerminal evaluatedTerminal = (Named.NamedTerminal) evaluateHighlevelParser(hlp, test);
		assertEquals(Terminal.Literal.class, evaluatedTerminal.getTerminal().getClass());
		assertEquals(test, evaluatedTerminal.getSymbol().getSymbol());
		assertEquals(test, evaluatedTerminal.getName());

		test = "--1'x}";
		evaluatedTerminal = (Named.NamedTerminal) evaluateHighlevelParser(hlp, test);
		assertEquals(Terminal.Literal.class, evaluatedTerminal.getTerminal().getClass());
		assertEquals(test, evaluatedTerminal.getSymbol().getSymbol());
		assertEquals(test, evaluatedTerminal.getName());

		test = ".";
		evaluatedTerminal = (Named.NamedTerminal) evaluateHighlevelParser(hlp, test);
		assertEquals(Terminal.Literal.class, evaluatedTerminal.getTerminal().getClass());
		assertEquals(test, evaluatedTerminal.getSymbol().getSymbol());
		assertEquals(test, evaluatedTerminal.getName());

		String testToFail = "lj{l";
		Parser hlp2 = hlp;
		assertThrows(RuntimeException.class, () -> { evaluateHighlevelParser(hlp2, testToFail); }, "Parsing failed");

	}

	@Test
	public void testExpression() {
		System.out.println("Test Expression");
		Parser hlp = new Parser();
		EBNF grammar = hlp.getGrammar();
		grammar.setWhatToMatch(hlp.EXPRESSION.getTarget());

		String test = "Today, let's wait for {time:int} minutes.";
		Named[] rhs = (Named[]) evaluateHighlevelParser(hlp, test);
		Rule myType = hlp.getTargetGrammar().sequence("mytype", rhs);

		// now parse and evaluate the generated grammar:
		EBNFCore tgt = hlp.getTargetGrammar();
		tgt.setWhatToMatch(myType.getTarget());
		RDParser rdParser = new RDParser(tgt.createBNF(), new Lexer("Today, let's wait for 5 minutes."), EBNFParsedNodeFactory.INSTANCE);
		DefaultParsedNode pn = rdParser.buildAst(rdParser.parse());
		if(pn.getMatcher().state != ParsingState.SUCCESSFUL)
			throw new RuntimeException();
		System.out.println(GraphViz.toVizDotLink(pn));
	}

	@Test
	public void testDefineType() {
		System.out.println("Test define type");
		Parser hlp = new Parser();
		hlp.defineType("percentage", "{p:int} %", pn -> pn.evaluate("p"));

		hlp.defineSentence("Now it is only {p:percentage}.", pn -> {
			int percentage = (int) pn.evaluate("p");
			System.out.println(percentage + " % left.");
			return null;
		});
		hlp.defineSentence("There is still {p:percentage} left.", pn -> {
			int percentage = (int) pn.evaluate("p");
			System.out.println(percentage + " % left.");
			return null;
		});

		ParsedNode pn = hlp.parse(
				"There is still 38 % left.\n" +
				"Now it is only 5 %.", null);
		pn.evaluate();
	}
}
