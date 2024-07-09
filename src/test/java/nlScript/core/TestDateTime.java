package nlScript.core;

import nlScript.ParseException;
import nlScript.ParsedNode;
import nlScript.Parser;
import nlScript.core.ParsingState;
import nlScript.ui.ACEditor;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestDateTime {
	@Test
	public void test01() throws ParseException {
		Parser hlp = new Parser();
		hlp.defineSentence("My daughter's school started {d:date-time}.", pn -> {
			LocalDateTime m = (LocalDateTime) pn.evaluate("d");
			assertEquals(m, LocalDateTime.of(2020, 9, 12, 8, 0));
			return null;
		});

		ParsedNode root = hlp.parse("My daughter's school started 12 September 2020 8:00.", null);
		assertEquals(ParsingState.SUCCESSFUL, root.getMatcher().state);
		root.evaluate();
	}

	public static void interactive() {
		Parser hlp = new Parser();
		hlp.defineSentence("My daughter's school started {d:date-time}.", pn -> {
			LocalDateTime m = (LocalDateTime) pn.evaluate("d");
			assertEquals(m, LocalDateTime.of(2020, 9, 12, 8, 0));
			return null;
		});
		ACEditor editor = new ACEditor(hlp);
		editor.setVisible(true);
	}

	public static void main(String[] args) {
		interactive();
	}
}
