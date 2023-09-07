package de.nls.ui;


import de.nls.Parser;
import de.nls.core.Autocompletion;

import java.util.ArrayList;

public class ACProvider {

	private final Parser parser;

	public ACProvider(Parser parser) {
		this.parser = parser;
	}

	public IAutocompletion[] getAutocompletions(String text) {
		ArrayList<Autocompletion> autocompletions = new ArrayList<>();
		parser.parse(text, autocompletions);
		return autocompletions.stream().map(ac -> new IAutocompletion() {
			@Override
			public String getAlreadyEnteredText() {
				return ac.getAlreadyEnteredText();
			}

			@Override
			public String getCompletion() {
				return ac.getCompletion();
			}
		}).toArray(IAutocompletion[]::new);
	}
}
