package nlScript.core;

import nlScript.ebnf.EBNFCore;

public interface Generator {

	Generation generate(EBNFCore grammar, GeneratorHints hints);
}
