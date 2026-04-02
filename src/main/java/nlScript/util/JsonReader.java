package nlScript.util;

import nlScript.core.Generation;
import nlScript.core.Named;
import nlScript.core.NonTerminal;
import nlScript.core.Terminal;
import nlScript.ebnf.EBNF;
import nlScript.ebnf.Join;
import nlScript.ebnf.Optional;
import nlScript.ebnf.Plus;
import nlScript.ebnf.Rule;
import nlScript.ebnf.Sequence;
import nlScript.ebnf.Star;

import java.util.ArrayList;
import java.util.Map;

public class JsonReader {

	private final Map<String, Rule> patternToRule;

	private final EBNF targetGrammar;

	public JsonReader(Map<String, Rule> patternToRule, EBNF targetGrammar) {
		this.patternToRule = patternToRule;
		this.targetGrammar = targetGrammar;
	}

	private String fromJsonObject(Json.JsonObject json) {
		String pattern = json.getAsString("signature");
		Rule rule = patternToRule.get(pattern);
		if (rule instanceof Sequence) {
			// append children consecutively
			// if Literal: append literal
			// if name == ws+: append ' '
			// otherwise: fromJson(child, json)
			Sequence sequence = (Sequence) rule;
			StringBuilder ret = new StringBuilder();
			for (Named<?> child : sequence.getChildren()) {
				ret.append(fromJson(child, json.get(child.getName())));
			}
			return ret.toString();
		} else {
			throw new RuntimeException("Expected sequence");
		}
	}

	/*
	 * Sequence -> JsonObject
	 * Plus -> JsonArray
	 * Optional -> JsonArray
	 * Star -> JsonArray
	 * Repeat -> JsonArray
	 * Join -> JsonArray
	 */
	public String fromJson(Named<?> namedSymbolOrRule, Json.JsonThing json) {
		if(json instanceof Json.JsonString) {
			return ((Json.JsonString) json).getValue();
		}

		if(namedSymbolOrRule.getName().equals("ws+"))
			return " ";

		Object symbol = namedSymbolOrRule.get();

		if(symbol instanceof Terminal.Literal)
			return ((Terminal.Literal) symbol).getLiteral();

		if(json instanceof Json.JsonObject) {
			return fromJsonObject((Json.JsonObject) json);
		}

		if(json instanceof Json.JsonArray) {
			ArrayList<Rule> ruleCandidates = targetGrammar.getRules((NonTerminal) symbol);
			if(ruleCandidates.size() != 1) {
				throw new RuntimeException("Don't know which rule to use");
			}
			Json.JsonArray jsonArray = (Json.JsonArray) json;
			Rule rule = ruleCandidates.get(0);
			Named<?> entry = null;
			if(rule instanceof Plus)          entry =     ((Plus) rule).getEntry();
			else if(rule instanceof Star)     entry =     ((Star) rule).getEntry();
			else if(rule instanceof Optional) entry = ((Optional) rule).getEntry();
			else if(rule instanceof Join)     entry =     ((Join) rule).getEntry();

			StringBuilder ret = new StringBuilder();

			if((rule instanceof Plus) || (rule instanceof Star) || (rule instanceof Optional)) {
				for (int i = 0; i < jsonArray.size(); i++)
					ret.append(fromJson(entry, jsonArray.get(i)));
				return ret.toString();
			}

			if (rule instanceof Join) {
				Join join = (Join) rule;
				Named<?> open = join.getOpen();
				Named<?> close = join.getClose();
				Named<?> delimiter = join.getDelimiter();

				if (join.hasOpen()) {
					Generation gen = join.generateChild(open.getName(), targetGrammar, open.getSymbol());
					ret.append(gen);
				}
				for (int i = 0; i < jsonArray.size(); i++) {
					ret.append(fromJson(entry, jsonArray.get(i)));
					if (join.hasDelimiter()) {
						Generation gen = join.generateChild(delimiter.getName(), targetGrammar, delimiter.getSymbol());
						ret.append(gen);
					}
				}
				if (join.hasClose()) {
					Generation gen = join.generateChild(close.getName(), targetGrammar, close.getSymbol());
					ret.append(gen);
				}
				return ret.toString();
			}
		}
		throw new RuntimeException();
	}
}
