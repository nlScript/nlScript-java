package de.nls;

import de.nls.core.ParsedNode;

public interface Evaluator {
	Object evaluate(ParsedNode pn);

	public static Evaluator ALL_CHILDREN_EVALUATOR = (pn -> {
		Object[] ret = new Object[pn.numChildren()];
		for(int i = 0; i < ret.length; i++)
			ret[i] = pn.evaluate(i);
		return ret;
	});
}
