package nlScript.ui;


import nlScript.ParseException;
import nlScript.Parser;
import nlScript.core.Autocompletion;

import java.util.ArrayList;
import java.util.List;

public class ACProvider {

	private final Parser parser;

	public ACProvider(Parser parser) {
		this.parser = parser;
	}

	public List<Autocompletion> getAutocompletions(String text) throws ParseException {
		ArrayList<Autocompletion> autocompletions = new ArrayList<>();
		parser.parse(text, autocompletions);
		return autocompletions;
	}

	public Parser getParser() {
		return parser;
	}
}
