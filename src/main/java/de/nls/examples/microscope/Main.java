package de.nls.examples.microscope;

import de.nls.Parser;
import de.nls.ui.ACEditor;

public class Main {
	public static void main(String[] args) {
		LanguageControl lc = new LanguageControl();
		Parser parser = lc.initParser();
		ACEditor editor = new ACEditor(parser);
		editor.setBeforeRun(lc::reset);
		editor.setAfterRun(() -> lc.getTimeline().process(Runnable::run));
		editor.setVisible(true);
	}
}
