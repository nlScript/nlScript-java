package de.nls.core;

import de.nls.ParseException;
import de.nls.ParsedNode;
import de.nls.Parser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestPath {

	@Test
	public void test01() throws ParseException {
		String homefolder = System.getProperty("user.home");
		System.out.println(homefolder);
		Parser hlp = new Parser();
		hlp.defineSentence("My home folder is {d:path}.", pn -> {
			String d = (String) pn.evaluate("d");
			assertEquals(d, homefolder);
			return null;
		});

		ParsedNode root = hlp.parse("My home folder is '" + homefolder + "'.", null);
		assertEquals(ParsingState.SUCCESSFUL, root.getMatcher().state);
		root.evaluate();
	}
}
