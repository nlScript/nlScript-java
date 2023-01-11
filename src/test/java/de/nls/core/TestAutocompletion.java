package de.nls.core;

import de.nls.Autocompleter;
import de.nls.ParsedNode;
import de.nls.ebnf.EBNFCore;
import de.nls.ebnf.Named;
import de.nls.ebnf.Rule;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class TestAutocompletion {

	@Test
	public void test01() {
		test("", "one ()");
		test("o", "one (o)");
		test("one", "${or} ()", "five ()");
		test("onet");
	}

	private void test(String input, String... expectedCompletion) {
		BNF grammar = makeGrammar();
		Lexer l = new Lexer(input);
		RDParser test = new RDParser(grammar, l);
		ArrayList<Autocompletion> autocompletions = new ArrayList<>();
		ParsedNode pn = test.parse(autocompletions);
		System.out.println(GraphViz.toVizDotLink(test.buildAst(pn)));

		System.out.println("input: " + input);
		System.out.println("completions: " + autocompletions);

		assertNotEquals(ParsingState.FAILED, pn.getMatcher().state);
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
				Named.n(BNF.literal("one")),
				Named.n("star", grammar.star(null,
						Named.n("or", grammar.or(null,
								Named.n(BNF.literal("two")),
								Named.n(BNF.literal("three")),
								Named.n(BNF.literal("four"))
						).setAutocompleter(pn -> {
							if(pn.getParsedString().length() > 0)
								return Autocompleter.VETO;
							return "${" + pn.getName() + "}";
						}))
				)),
				Named.n(BNF.literal("five"))
		);

		grammar.setWhatToMatch(e.getTarget());
		return grammar.createBNF();
	}
}
