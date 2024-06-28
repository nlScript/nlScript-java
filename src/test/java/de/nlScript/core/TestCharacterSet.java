package de.nlScript.core;

import de.nlScript.ParseException;
import de.nlScript.ParsedNode;
import de.nlScript.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCharacterSet {

	@Test
	public void test01() throws ParseException {
		Parser hlp = new Parser();
		hlp.defineSentence("An arbitrary alphanumeric character: {c:[a-zA-Z0-9]}.", pn -> {
			Character d = (Character) pn.evaluate("c");
			assertEquals(d, 'f');
			return null;
		});

		hlp.defineSentence("Two arbitrary alphanumeric characters: {c:[a-zA-Z0-9]:2}.", pn -> {
			Object[] d = (Object[]) pn.evaluate("c");
			assertArrayEquals(d, new Character[] { 'f', '1' });
			return null;
		});

		ParsedNode root = hlp.parse("An arbitrary alphanumeric character: f.", null);
		assertEquals(ParsingState.SUCCESSFUL, root.getMatcher().state);
		root.evaluate();

		root = hlp.parse("Two arbitrary alphanumeric characters: f1.", null);
		assertEquals(ParsingState.SUCCESSFUL, root.getMatcher().state);
		root.evaluate();
	}
}
