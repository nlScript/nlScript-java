package de.nlScript.core;

import de.nlScript.ParseException;
import de.nlScript.ParsedNode;
import de.nlScript.Parser;
import de.nlScript.ui.ACEditor;
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

	public static void interactive() {
		String homefolder = System.getProperty("user.home");
		System.out.println(homefolder);
		Parser hlp = new Parser();
		hlp.defineSentence("My home folder is {d:path}.", pn -> null);

		ACEditor editor = new ACEditor(hlp);
		editor.setVisible(true);
	}

	public static void main(String[] args) {
		interactive();
	}
}
