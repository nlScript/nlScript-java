package de.nls.core;

import de.nls.ParseException;
import de.nls.ParsedNode;
import de.nls.Parser;
import de.nls.ebnf.EBNFCore;
import de.nls.ebnf.EBNFParsedNodeFactory;
import de.nls.ebnf.Rule;
import de.nls.ui.ACEditor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class TestAutocompletion {

	@Test
	public void test01() throws ParseException {
		test("", "one ()");
		test("o", "one (o)");
		test("one", "${or} ()", "five ()");
		test("onet");
	}

	@Test
	public void test02() throws ParseException {
		Parser parser = new Parser();
		parser.defineSentence("The first digit of the number is {first:digit}.", pn -> null);
		ArrayList<Autocompletion> autocompletions = new ArrayList<>();
		parser.parse("The first digit of the number is ", autocompletions);
		assertEquals(1, autocompletions.size());
		assertEquals("${first}", autocompletions.get(0).getCompletion(Autocompletion.Purpose.FOR_INSERTION));
	}

	@Test
	public void test03() throws ParseException {
		Parser parser = new Parser();
		parser.defineSentence("Define the output path {p:path}.", pn -> null);
		ArrayList<Autocompletion> autocompletions = new ArrayList<>();
		parser.parse("", autocompletions);
		assertEquals(2, autocompletions.size());
		assertEquals("Define the output path", autocompletions.get(1).getCompletion(Autocompletion.Purpose.FOR_INSERTION));
	}

	@Test
	public void test04() throws ParseException {
		ArrayList<String> sentencesParsed = new ArrayList<>();

		Parser parser = new Parser();
		parser.addParseStartListener(() -> {
			sentencesParsed.clear();
			System.out.println("sentencesParsed clear");
		});
		parser.defineSentence("{d:digit:+}.", pn -> null).onSuccessfulParsed(pn -> {
			System.out.println("Successfully parsed " + pn.getParsedString() + " by " + parser);
			sentencesParsed.add(pn.getParsedString());
		});

		ArrayList<Autocompletion> autocompletions = new ArrayList<>();
		parser.parse("1.22.333.", autocompletions);

		ArrayList<String> expected = new ArrayList<>(Arrays.asList("1.", "22.", "333."));
		assertEquals(expected, sentencesParsed);
	}

	@Test
	public void test05() throws ParseException {
		final ArrayList<String> definedChannels = new ArrayList<>();

		Parser parser = new Parser();
		parser.addParseStartListener(() -> {
			System.out.println("Clear defined channels");
			definedChannels.clear();
		});

		// parser.defineType("channel-name", "'{<name>:[A-Za-z0-9]:+}'", e -> null, new Autocompleter.IfNothingYetEnteredAutocompleter("'${name}'"));

		parser.defineSentence(
				"Define channel {channel-name:[A-Za-z0-9]:+}.",
				e -> null
		).onSuccessfulParsed(n -> {
			System.out.println("Successfully parsed " + n.getParsedString("channel-name"));
			definedChannels.add(n.getParsedString("channel-name"));
		});

		parser.defineType("defined-channels", "'{channel:[A-Za-z0-9]:+}'",
				e -> null,
				(e, justCheck) -> Autocompletion.literal(e, definedChannels));

		parser.defineSentence(
				"Use channel {channel:defined-channels}.",
				e -> null
		);

		ArrayList<Autocompletion> autocompletions = new ArrayList<>();
		ParsedNode root = parser.parse(
				"Define channel DAPI.\n" +
				"Define channel A488.\n" +
				"Use channel 'DAPI'.\n" +
				"Use channel 'A488'.\n" +
				"Use channel ", autocompletions);
		System.out.println(GraphViz.toVizDotLink(root));
		assertEquals(ParsingState.END_OF_INPUT, root.getMatcher().state);

		ArrayList<String> expected = new ArrayList<>();
		expected.add("DAPI");
		expected.add("A488");

		assertEquals(expected, autocompletions.stream().map(ac -> ac.getCompletion(Autocompletion.Purpose.FOR_INSERTION)).collect(Collectors.toList()));
	}

	@Test
	public void test06() throws ParseException {
		EBNFCore ebnf = new EBNFCore();

		Rule sentence = ebnf.sequence("sentence",
				Terminal.literal("Define channel").withName(),
				Terminal.WHITESPACE.withName("ws"),
				ebnf.plus("name",
						Terminal.characterClass("[A-Za-z]").withName()
				).withName("name"),
				Terminal.literal(".").withName());
		Rule program = ebnf.star("program",
				// Named.n("sentence", sentence));
				new NonTerminal("sentence").withName("sentence"));

		ebnf.compile(program.getTarget());

		String text = "Define channel DA.D";

		ArrayList<Autocompletion> autocompletions = new ArrayList<>();

		BNF bnf = ebnf.getBNF();
		System.out.println(bnf);
		RDParser parser = new RDParser(bnf, new Lexer(text), EBNFParsedNodeFactory.INSTANCE);
		ParsedNode pn = (ParsedNode) parser.parse(autocompletions);
		System.out.println(GraphViz.toVizDotLink(pn));
		System.out.println(pn.getMatcher().state);
		assertEquals(ParsingState.END_OF_INPUT, pn.getMatcher().state);
		assertEquals(1, autocompletions.size());
		assertEquals("Define channel", autocompletions.get(0).getCompletion(Autocompletion.Purpose.FOR_INSERTION));
	}

	@Test
	public void test07() throws ParseException {
		Parser parser = new Parser();

		parser.defineType("led", "385nm", e -> null, (e, justCheck) -> Autocompletion.literal(e, "385nm"));
		parser.defineType("led", "470nm", e -> null, (e, justCheck) -> Autocompletion.literal(e, "470nm"));
		parser.defineType("led", "567nm", e -> null, (e, justCheck) -> Autocompletion.literal(e, "567nm"));
		parser.defineType("led", "625nm", e -> null, (e, justCheck) -> Autocompletion.literal(e, "625nm"));

		parser.defineType("led-power", "{<led-power>:int}%", e -> null,true);
		parser.defineType("led-setting", "{led-power:led-power} at {wavelength:led}", e -> null,true);

		parser.defineSentence(
				"Excite with {led-setting:led-setting}.",
				e ->null);

		ArrayList<Autocompletion> autocompletions = new ArrayList<>();
		ParsedNode root = parser.parse("Excite with 10% at 3", autocompletions);
		assertEquals(ParsingState.END_OF_INPUT, root.getMatcher().state);
		assertEquals(1, autocompletions.size());
		assertEquals("385nm", autocompletions.get(0).getCompletion(Autocompletion.Purpose.FOR_INSERTION));

		autocompletions.clear();
		root = parser.parse("Excite with 10% at ", autocompletions);
		System.out.println(autocompletions);
	}

	public static void main(String[] args) throws ParseException {
		Parser parser = new Parser();

		parser.defineType("led", "385nm", e -> null, (e, justCheck) -> Autocompletion.literal(e, "385nm"));
		parser.defineType("led", "470nm", e -> null, (e, justCheck) -> Autocompletion.literal(e, "470nm"));
		parser.defineType("led", "567nm", e -> null, (e, justCheck) -> Autocompletion.literal(e, "567nm"));
		parser.defineType("led", "625nm", e -> null, (e, justCheck) -> Autocompletion.literal(e, "625nm"));

		parser.defineType("led-power", "{<led-power>:int}%", e -> null,true);
		parser.defineType("led-setting", "{led-power:led-power} at {wavelength:led}", e -> null,true);

		parser.defineSentence(
				"Excite with {led-setting:led-setting}.",
				e ->null);

		ArrayList<Autocompletion> autocompletions = new ArrayList<>();
		ParsedNode root = parser.parse("Excite with 10% at 3", autocompletions);
		assertEquals(ParsingState.END_OF_INPUT, root.getMatcher().state);
		assertEquals(1, autocompletions.size());
		assertEquals("385nm", autocompletions.get(0).getCompletion(Autocompletion.Purpose.FOR_INSERTION));

		autocompletions.clear();
		root = parser.parse("Excite with 10% at ", autocompletions);
		System.out.println(autocompletions);

		ACEditor editor = new ACEditor(parser);
		editor.setVisible(true);
	}

	@Test
	public void test08() throws ParseException {
		Parser parser = new Parser();

		parser.defineType("my-color", "blue", null);
		parser.defineType("my-color", "green", null);
		parser.defineType("my-color", "({r:int}, {g:int}, {b:int})", null, true);
		parser.defineSentence("My favorite color is {color:my-color}.", null);

		ArrayList<Autocompletion> autocompletions = new ArrayList<>();
		ParsedNode root = parser.parse("My favorite color is ", autocompletions);
		assertEquals(ParsingState.END_OF_INPUT, root.getMatcher().state);
		assertEquals(3, autocompletions.size());
		assertEquals("blue", autocompletions.get(0).getCompletion(Autocompletion.Purpose.FOR_INSERTION));
		assertEquals("green", autocompletions.get(1).getCompletion(Autocompletion.Purpose.FOR_INSERTION));
		assertEquals("(${r}, ${g}, ${b})", autocompletions.get(2).getCompletion(Autocompletion.Purpose.FOR_INSERTION));
	}

	@Test
	public void test09() throws ParseException {
		Parser parser = new Parser();
		parser.defineType("channel-name", "'{<name>:[A-Za-z0-9]:+}'",
				e -> e.getParsedString("<name>"),
				true);

		parser.defineSentence(
				"Define channel {channel-name:channel-name}.",
				e -> null);

		ArrayList<Autocompletion> autocompletions = new ArrayList<>();
		ParsedNode root = parser.parse("Define channel 'D", autocompletions);

		System.out.println("autocompletions = " + autocompletions.stream().map(ac -> ac.getCompletion(Autocompletion.Purpose.FOR_INSERTION)).collect(Collectors.toList()));

	}

	private void test(String input, String... expectedCompletion) throws ParseException {
		System.out.println("Testing " + input);
		BNF grammar = makeGrammar();
		Lexer l = new Lexer(input);
		RDParser test = new RDParser(grammar, l, EBNFParsedNodeFactory.INSTANCE);
		ArrayList<Autocompletion> autocompletions = new ArrayList<>();
		DefaultParsedNode pn = test.parse(autocompletions);
		System.out.println(GraphViz.toVizDotLink(pn));

		System.out.println("input: " + input);
		System.out.println("completions: " + autocompletions);

		assertEquals(ParsingState.END_OF_INPUT, pn.getMatcher().state);
		assertArrayEquals(expectedCompletion, getCompletionStrings(autocompletions));
	}

	private String[] getCompletionStrings(ArrayList<Autocompletion> autocompletions) {
		String[] ret = new String[autocompletions.size()];
		for(int i = 0; i < ret.length; i++) {
			ret[i] = autocompletions.get(i).getCompletion(Autocompletion.Purpose.FOR_INSERTION) + " (" + autocompletions.get(i).getAlreadyEntered() + ")";
		}
		return ret;
	}

	/**
	 * 'one' ('two' | 'three' | 'four')* 'five'
	 */
	private static BNF makeGrammar() {
		EBNFCore grammar = new EBNFCore();
		Rule e = grammar.sequence("expr",
				Terminal.literal("one").withName(),
				grammar.star(null,
						grammar.or(null,
								Terminal.literal("two").withName(),
								Terminal.literal("three").withName(),
								Terminal.literal("four").withName()
						).setAutocompleter((pn, justCheck) -> {
							if(!pn.getParsedString().isEmpty())
								return Autocompletion.veto(pn);
							return Autocompletion.parameterized(pn, pn.getName());
						}).withName("or")
				).withName("star"),
				Terminal.literal("five").withName()
		);

		grammar.compile(e.getTarget());
		return grammar.getBNF();
	}
}
