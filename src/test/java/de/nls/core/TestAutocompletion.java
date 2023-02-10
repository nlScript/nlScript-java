package de.nls.core;

import de.nls.Autocompleter;
import de.nls.ParsedNode;
import de.nls.Parser;
import de.nls.ebnf.EBNFCore;
import de.nls.ebnf.EBNFParsedNodeFactory;
import de.nls.ebnf.Named;
import de.nls.ebnf.Rule;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class TestAutocompletion {

	@Test
	public void test01() {
		test("", "one ()");
		test("o", "one (o)");
		test("one", "${or} ()", "five ()");
		test("onet");
	}

	@Test
	public void test02() {
		Parser parser = new Parser();
		parser.defineSentence("The first digit of the number is {first:digit}.", pn -> null);
		ArrayList<Autocompletion> autocompletions = new ArrayList<>();
		parser.parse("The first digit of the number is ", autocompletions);
		assertEquals(1, autocompletions.size());
		assertEquals(new Autocompletion("${first}", ""), autocompletions.get(0));
	}

	@Test
	public void test03() {
		Parser parser = new Parser();
		parser.defineSentence("Define the output path {p:path}.", pn -> null);
		ArrayList<Autocompletion> autocompletions = new ArrayList<>();
		parser.parse("Define the output path ", autocompletions);
		assertEquals(1, autocompletions.size());
		assertEquals(new Autocompletion("Define the output path", ""), autocompletions.get(0));
	}

	@Test
	public void test04() {
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
		DefaultParsedNode pn = parser.parse("1.22.333.", autocompletions);

		ArrayList<String> expected = new ArrayList<>(Arrays.asList("1.", "22.", "333."));
		assertEquals(expected, sentencesParsed);
	}

	@Test
	public void test05() {
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
				e -> String.join(";;;", definedChannels));

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

		ArrayList<Autocompletion> expected = new ArrayList<>();
		expected.add(new Autocompletion("DAPI", ""));
		expected.add(new Autocompletion("A488", ""));

		assertEquals(expected, autocompletions);

	}

	@Test
	public void test06() {
		EBNFCore ebnf = new EBNFCore();

		Rule sentence = ebnf.sequence("sentence",
				Named.n(Terminal.literal("Define channel")),
				Named.n("ws", Terminal.WHITESPACE),
				Named.n("name", ebnf.plus("name",
						Named.n(Terminal.characterClass("[A-Za-z]"))
				)),
				Named.n(Terminal.literal(".")));
		Rule program = ebnf.star("program",
				// Named.n("sentence", sentence));
				Named.n("sentence", new NonTerminal("sentence")));

		ebnf.setWhatToMatch(program.getTarget());

		String text = "Define channel DA.D";

		ArrayList<Autocompletion> autocompletions = new ArrayList<>();

		BNF bnf = ebnf.createBNF();
		System.out.println(bnf);
		RDParser parser = new RDParser(bnf, new Lexer(text), EBNFParsedNodeFactory.INSTANCE);
		ParsedNode pn = (ParsedNode) parser.parse(autocompletions);
		System.out.println(GraphViz.toVizDotLink(pn));
		System.out.println(pn.getMatcher().state);
		assertEquals(ParsingState.END_OF_INPUT, pn.getMatcher().state);

	}

	@Test
	public void test07() {
		Parser parser = new Parser();

		parser.defineType("led", "385nm", e -> null, e -> "385nm");
		parser.defineType("led", "470nm", e -> null, e -> "470nm");
		parser.defineType("led", "567nm", e -> null, e -> "567nm");
		parser.defineType("led", "625nm", e -> null, e -> "625nm");

		parser.defineType("led-power", "{<led-power>:int}%", e ->null,true);
		parser.defineType("led-setting", "{led-power:led-power} at {wavelength:led}", e ->null,true);

		parser.defineSentence(
				"Excite with {led-setting:led-setting}.",
				e ->null);

		ArrayList<Autocompletion> autocompletions = new ArrayList<>();
		ParsedNode root = parser.parse("Excite with 10% at 3", autocompletions);
		assertEquals(ParsingState.END_OF_INPUT, root.getMatcher().state);
	}


	private void test(String input, String... expectedCompletion) {
		System.out.println("Testing " + input);
		BNF grammar = makeGrammar();
		Lexer l = new Lexer(input);
		RDParser test = new RDParser(grammar, l, EBNFParsedNodeFactory.INSTANCE);
		ArrayList<Autocompletion> autocompletions = new ArrayList<>();
		DefaultParsedNode pn = test.parse(autocompletions);
		System.out.println(GraphViz.toVizDotLink(test.buildAst(pn)));

		System.out.println("input: " + input);
		System.out.println("completions: " + autocompletions);

		assertEquals(ParsingState.END_OF_INPUT, pn.getMatcher().state);
		assertArrayEquals(expectedCompletion, getCompletionStrings(autocompletions));
	}

	private String[] getCompletionStrings(ArrayList<Autocompletion> autocompletions) {
		String[] ret = new String[autocompletions.size()];
		for(int i = 0; i < ret.length; i++) {
			ret[i] = autocompletions.get(i).getCompletion() + " (" + autocompletions.get(i).getAlreadyEnteredText() + ")";
		}
		return ret;
	}

	/**
	 * 'one' ('two' | 'three' | 'four')* 'five'
	 */
	private static BNF makeGrammar() {
		EBNFCore grammar = new EBNFCore();
		Rule e = grammar.sequence("expr",
				Named.n(Terminal.literal("one")),
				Named.n("star", grammar.star(null,
						Named.n("or", grammar.or(null,
								Named.n(Terminal.literal("two")),
								Named.n(Terminal.literal("three")),
								Named.n(Terminal.literal("four"))
						).setAutocompleter(pn -> {
							if(pn.getParsedString().length() > 0)
								return Autocompleter.VETO;
							return "${" + pn.getName() + "}";
						}))
				)),
				Named.n(Terminal.literal("five"))
		);

		grammar.setWhatToMatch(e.getTarget());
		return grammar.createBNF();
	}
}
