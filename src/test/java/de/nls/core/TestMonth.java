package de.nls.core;

import de.nls.ParseException;
import de.nls.ParsedNode;
import de.nls.Parser;
import de.nls.ui.ACEditor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMonth {

	@Test
	public void test01() throws ParseException {
		Parser hlp = new Parser();
		hlp.defineSentence("My birthday is in {m:month}.", pn -> {
			int m = (Integer) pn.evaluate("m");
			assertEquals(m, 2);
			return null;
		});

		ParsedNode root = hlp.parse("My birthday is in March.", null);
		assertEquals(ParsingState.SUCCESSFUL, root.getMatcher().state);
		root.evaluate();

		ArrayList<Autocompletion> autocompletions = new ArrayList<>();
		hlp.parse("My birthday is in ", autocompletions);
		System.out.println("autocompletions = " + autocompletions);
	}

	public static void interactive() {
		Parser hlp = new Parser();
		hlp.defineSentence("My birthday is in {m:month}.", pn -> {
			int m = (Integer) pn.evaluate("m");
			assertEquals(m, 2);
			return null;
		});
		ACEditor editor = new ACEditor(hlp);
		editor.setVisible(true);
	}

	public static void main(String[] args) {
		 interactive();
	}
}
