package nlScript.core;

import nlScript.ebnf.EBNFCore;

public interface Generatable {
	Generation generate(EBNFCore grammar);
}
