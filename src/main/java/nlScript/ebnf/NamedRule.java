package nlScript.ebnf;

import nlScript.core.Generator;
import nlScript.core.Named;

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

	public NamedRule setGenerator(Generator g) {
		get().setGenerator(g);
		return this;
	}

	public Generator getGenerator() {
		return get().getGenerator();
	}

	public NamedRule setGeneratorWithDescription(String description) {
		return setGeneratorWithDescription(description, false);
	}

	public NamedRule setGeneratorWithDescription(String description, boolean skipVariableSubstitution) {
		setGenerator(getGenerator().withDescription(description, skipVariableSubstitution));
		return this;
	}

	public NamedRule setGeneratorFromChild(String child) {
		setGenerator(getGenerator().fromChild(child));
		return this;
	}
}