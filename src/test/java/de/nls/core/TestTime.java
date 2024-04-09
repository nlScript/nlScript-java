package de.nls.core;

import de.nls.ParseException;
import de.nls.ParsedNode;
import de.nls.Parser;
import de.nls.ui.ACEditor;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestTime {
	@Test
	public void test01() throws ParseException {
		Parser hlp = new Parser();
		hlp.defineSentence("The pizza comes at {t:time}.", pn -> {
			LocalTime time = (LocalTime) pn.evaluate("t");
			assertEquals(LocalTime.of(20, 30), time);
			return null;
		});

		ParsedNode root = hlp.parse("The pizza comes at 20:30.", null);
		assertEquals(ParsingState.SUCCESSFUL, root.getMatcher().state);
		root.evaluate();
	}

	public static void interactive() {
		Parser hlp = new Parser();
		hlp.defineSentence("The pizza comes at {t:time}.", pn -> {
			LocalTime time = (LocalTime) pn.evaluate("t");
			assertEquals(LocalTime.of(20, 30), time);
			return null;
		});
		new ACEditor(hlp).setVisible(true);
	}

	public static void main(String[] args) {
		interactive();
	}
}
