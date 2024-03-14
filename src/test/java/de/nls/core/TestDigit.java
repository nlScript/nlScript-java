package de.nls.core;

import de.nls.ParseException;
import de.nls.ParsedNode;
import de.nls.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestDigit {

	@Test
	public void test01() throws ParseException {
		Parser hlp = new Parser();
		hlp.defineSentence("The first digit of my telephone number is {d:digit}.", pn -> {
			Character d = (Character) pn.evaluate("d");
			assertEquals(d, '0');
			return null;
		});

		hlp.defineSentence("The first two digits of my telephone number are {d:digit:2}.", pn -> {
			String d = (String) pn.evaluate("d");
			assertEquals(d, "09");
			return null;
		});

		ParsedNode root = hlp.parse("The first digit of my telephone number is 0.", null);
		assertEquals(ParsingState.SUCCESSFUL, root.getMatcher().state);
		root.evaluate();

		root = hlp.parse("The first two digits of my telephone number are 09.", null);
		assertEquals(ParsingState.SUCCESSFUL, root.getMatcher().state);
		root.evaluate();
	}
}
