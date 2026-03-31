package nlScript.core;

import nlScript.ebnf.EBNFCore;

public interface Generator {

	Generation generate(EBNFCore grammar, GeneratorHints hints);

	default Generator fromChild(String child) {
		final Generator myself = this;
		return  (grammar, hints) -> myself.generate(grammar, hints).getChild(child);
	}
}
