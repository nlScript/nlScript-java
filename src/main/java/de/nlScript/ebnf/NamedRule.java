package de.nlScript.ebnf;

import de.nlScript.core.Named;

public class NamedRule extends Named<Rule> {
	public NamedRule(Rule object, String name) {
		super(object, name);
	}

	public NamedRule(Rule object) {
		super(object);
	}

	public void onSuccessfulParsed(ParseListener listener) {
		get().onSuccessfulParsed(listener);
	}
}
