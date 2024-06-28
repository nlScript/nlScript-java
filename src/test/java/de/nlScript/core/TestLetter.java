package de.nlScript.core;

import de.nlScript.ParseException;
import de.nlScript.ParsedNode;
import de.nlScript.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestLetter {

	@Test
	public void test01() throws ParseException {
		Parser hlp = new Parser();
		hlp.defineSentence("The first character of my name is {l:letter}.", pn -> {
			char l = (char) pn.evaluate("l");
			assertEquals(l, 'B');
			return null;
		});

		hlp.defineSentence("The first two characters of my name are {l:letter:2}.", pn -> {
			Object[] l = (Object[]) pn.evaluate("l");
			assertArrayEquals(l, new Character[] { 'B', 'e' });
			return null;
		});

		ParsedNode root = hlp.parse("The first character of my name is B.", null);
		assertEquals(ParsingState.SUCCESSFUL, root.getMatcher().state);
		root.evaluate();

		root = hlp.parse("The first two characters of my name are Be.", null);
		assertEquals(ParsingState.SUCCESSFUL, root.getMatcher().state);
		root.evaluate();
	}
}
