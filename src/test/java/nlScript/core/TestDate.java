package nlScript.core;

import nlScript.ParseException;
import nlScript.ParsedNode;
import nlScript.Parser;
import nlScript.core.ParsingState;
import nlScript.ui.ACEditor;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestDate {
	@Test
	public void test01() throws ParseException {
		Parser hlp = new Parser();
		hlp.defineSentence("My cat was born on {d:date}.", pn -> {
			LocalDate m = (LocalDate) pn.evaluate("d");
			assertEquals(m, LocalDate.of(2020, 10, 3));
			return null;
		});

		ParsedNode root = hlp.parse("My cat was born on 03 October 2020.", null);
		assertEquals(ParsingState.SUCCESSFUL, root.getMatcher().state);
		root.evaluate();
	}

	public static void interactive() {
		Parser hlp = new Parser();
		hlp.defineSentence("My cat was born on {d:date}.", pn -> {
			LocalDate m = (LocalDate) pn.evaluate("d");
			assertEquals(m, LocalDate.of(2020, 10, 3));
			return null;
		});
		ACEditor editor = new ACEditor(hlp);
		editor.setVisible(true);
	}

	public static void main(String[] args) {
		interactive();
	}
}
