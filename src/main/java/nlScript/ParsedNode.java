package nlScript;

import nlScript.core.Autocompletion;
import nlScript.core.DefaultParsedNode;
import nlScript.ebnf.EBNFProduction;
import nlScript.ebnf.ParseListener;
import nlScript.ebnf.Rule;
import nlScript.core.Matcher;
import nlScript.core.ParsingState;
import nlScript.core.Production;
import nlScript.core.Symbol;

public class ParsedNode extends DefaultParsedNode {

	private int nthEntryInParent = 0;

	public ParsedNode(Matcher matcher, Symbol symbol, Production production) {
		super(matcher, symbol, production);
	}

	public void setNthEntryInParent(int nthEntry) {
		this.nthEntryInParent = nthEntry;
	}

	public int getNthEntryInParent() {
		return nthEntryInParent;
	}

	public Rule getRule() {
		Production production = getProduction();
		if(production != null && production instanceof EBNFProduction)
			return ((EBNFProduction) production).getRule();
		return null;
	}

	private boolean parentHasSameRule() {
		Rule thisRule = getRule();
		if(thisRule == null)
			return false;
		DefaultParsedNode parent = getParent();
		if(parent == null)
			return false;
		Rule parentRule = ((ParsedNode) parent).getRule();
		if(parentRule == null)
			return false;
		return thisRule.equals(parentRule);
	}

	public Autocompletion[] getAutocompletion(boolean justCheck) {
		Rule rule = getRule();
		if(rule != null && rule.getAutocompleter() != null && !parentHasSameRule()) {
			return rule.getAutocompleter().getAutocompletion(this, justCheck);
		}
		return super.getAutocompletion(justCheck);
	}

	public void notifyListeners() {
		for(int i = 0; i < numChildren(); i++)
			((ParsedNode) getChild(i)).notifyListeners();

		ParsingState state = getMatcher().state;
		if (state != ParsingState.SUCCESSFUL && state != ParsingState.END_OF_INPUT)
			return;
		Rule rule = getRule();
		if (rule != null && !parentHasSameRule()) {
			ParseListener l = rule.getOnSuccessfulParsed();
			if (l != null)
				l.parsed(this);
		}
	}

	public Object evaluate() {
		Rule rule = getRule();
		if(rule != null && rule.getEvaluator() != null) {
			return rule.getEvaluator().evaluate(this);
		}
		return super.evaluate();
	}
}
