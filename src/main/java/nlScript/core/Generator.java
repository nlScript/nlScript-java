package nlScript.core;

import nlScript.ebnf.EBNFCore;

public interface Generator {

	Generation generate(EBNFCore grammar, GeneratorHints hints);

	default Generator withDescription(String description, boolean skipVariableSubstitution) {
		final Generator myself = this;
		return (grammar, hints) -> {
			Generation gen = myself.generate(grammar, hints);
			String desc = description;
			if(!skipVariableSubstitution)
				desc = gen.processText(desc);
			gen.setDescription(desc);
			return gen;
		};
	}

	default Generator fromChild(String child) {
		final Generator myself = this;
		return  (grammar, hints) -> myself.generate(grammar, hints).getChild(child);
	}
}
