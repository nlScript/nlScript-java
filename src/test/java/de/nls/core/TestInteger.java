package de.nls.core;

import de.nls.ParseException;
import de.nls.ParsedNode;
import de.nls.Parser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestInteger {

	@Test
	public void test01() throws ParseException {
		Parser hlp = new Parser();
		hlp.defineSentence("Now there are only {p:int}% left.", pn -> {
			int p = (Integer) pn.evaluate("p");
			assertEquals(35, p);
			return null;
		});

		ArrayList<Autocompletion> autocompletions = new ArrayList<>();

		ParsedNode root = hlp.parse("Now there are only 5", autocompletions);
		assertEquals(ParsingState.END_OF_INPUT, root.getMatcher().state);
		assertEquals(0, autocompletions.size());

		root = hlp.parse("Now there are only 35% left.", autocompletions);
		assertEquals(ParsingState.SUCCESSFUL, root.getMatcher().state);
		root.evaluate();
	}
}
