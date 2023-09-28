package de.nls.core;

import de.nls.ParseException;
import de.nls.ParsedNode;
import de.nls.Parser;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestColor {

	@Test
	public void test01() throws ParseException {
		Parser hlp = new Parser();
		hlp.defineSentence("My favorite color is {c:color}.", pn -> {
			int color = (Integer) pn.evaluate("c");
			assertEquals(new Color(128, 255, 0).getRGB(), color);
			return null;
		});

		ArrayList<Autocompletion> autocompletions = new ArrayList<>();
		hlp.parse("My favorite color is ", autocompletions);

		String[] actual = autocompletions.stream().map(Autocompletion::getCompletion).toArray(String[]::new);
		String[] expected = new String[] {
			"(${red}, ${green}, ${blue})",
			"black",
			"white",
			"red",
			"orange",
			"yellow",
			"lawn green",
			"green",
			"spring green",
			"cyan",
			"azure",
			"blue",
			"violet",
			"magenta",
			"pink",
			"gray" };

		assertArrayEquals(expected, actual);

		ParsedNode root = hlp.parse("My favorite color is lawn green.", null);
		assertEquals(ParsingState.SUCCESSFUL, root.getMatcher().state);
		root.evaluate();
	}
}
