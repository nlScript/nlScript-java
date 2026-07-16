package nlScript.ebnf;

import nlScript.core.GenerationListener;
import nlScript.core.Generator;
import nlScript.core.GeneratorHints;
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

	public void setGenerationListener(GenerationListener listener) {
		get().setGenerationListener(listener);
	}

	public void setChildGenerationListener(String childName, GenerationListener listener) {
		get().setChildGenerationListener(childName, listener);
	}

	public NamedRule setGenerator(Generator g) {
		get().setGenerator(g);
		return this;
	}

	public Generator getGenerator() {
		return get().getGenerator();
	}

	public NamedRule setGeneratorDescription(String desc) {
		get().setGeneratorHints(GeneratorHints.from(GeneratorHints.Key.DESCRIPTION, desc));
		return this;
	}

	public NamedRule setGeneratorFromChild(String child) {
		setGenerator(getGenerator().fromChild(child));
		return this;
	}
}