package de.nls;

import de.nls.core.DefaultParsedNode;

import java.util.Arrays;

public interface Evaluator {
	Object evaluate(ParsedNode pn);

	Evaluator ALL_CHILDREN_EVALUATOR = (pn -> {
		Object[] ret = new Object[pn.numChildren()];
		if(ret.length == 0)
			return ret;

		boolean allAreCharacters = true;
		for (int i = 0; i < ret.length; i++) {
			ret[i] = pn.evaluate(i);
			allAreCharacters = allAreCharacters && (ret[i] instanceof Character);
		}
		if(!allAreCharacters)
			return ret;

//		StringBuilder b = new StringBuilder();
//		Arrays.stream(ret).forEach(b::append);
//		return b.toString();
		return ret;
	});

	Evaluator FIRST_CHILD_EVALUATOR = (pn -> pn.evaluate(0));

	Evaluator DEFAULT_EVALUATOR = (DefaultParsedNode::getParsedString);
}
