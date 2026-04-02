package nlScript.util;

import nlScript.JsonSerializer;
import nlScript.ParsedNode;
import nlScript.core.Named;
import nlScript.core.Terminal;
import nlScript.ebnf.Join;
import nlScript.ebnf.Optional;
import nlScript.ebnf.Plus;
import nlScript.ebnf.Rule;
import nlScript.ebnf.Sequence;
import nlScript.ebnf.Star;

public class JsonWriter {

	public static Json.JsonObject writeSentence(Sequence sentence, ParsedNode pn, String signature) {
		Named<?>[] rhs = sentence.getChildren();

		Json.JsonObject json = new Json.JsonObject();
		json.put("signature", new Json.JsonString(signature));

		for (int i = 0; i < rhs.length; i++) {
			// each entry is either a variable or a literal
			// if it's a variable, it's either a
			// - a symbol in the target grammar,
			// - a join rule (if type is tuple<> or list<> or the quantifier is given in discrete numbers
			// - a star rule
			// - a optional rule
			// - a plus rule
			String name = rhs[i].getName();

			ParsedNode nodeForChild = (ParsedNode) pn.getChild(i); // it's a sequence, so we can use the index, as there will be one ParsedNode per RHS symbol

			Rule childRule = nodeForChild.getRule();

			if (nodeForChild.getSymbol() instanceof Terminal.Literal)
				continue; // skip non-variables

			if (name.equals("ws+"))
				continue;

			if (((childRule instanceof Join) && !(((Join) childRule).getEntry().get() instanceof Terminal.CharacterClass)))
				json.put(name, serializeCompound(nodeForChild));
			else if (((childRule instanceof Star) && !(((Star) childRule).getEntry().get() instanceof Terminal.CharacterClass)))
				json.put(name, serializeCompound(nodeForChild));
			else if (((childRule instanceof Plus) && !(((Plus) childRule).getEntry().get() instanceof Terminal.CharacterClass)))
				json.put(name, serializeCompound(nodeForChild));
			else if (((childRule instanceof Optional) && !(((Optional) childRule).getEntry().get() instanceof Terminal.CharacterClass)))
				json.put(name, serializeCompound(nodeForChild));
			else {
				json.putIfNotNull(name, serializeSingle(nodeForChild));
			}
		}
		return json;
	}

	private static Json.JsonThing serializeSingle(ParsedNode node) {
		Rule rule = node.getRule();
		JsonSerializer serializer = rule == null ? null : rule.getJsonSerializer();
		if (serializer != null) {
			return serializer.toJSON(node);
		} else {
			return new Json.JsonString(node.getParsedString());
		}
	}

	private static Json.JsonArray serializeCompound(ParsedNode node) {
		Json.JsonArray jsonArray = new Json.JsonArray();
		for (int j = 0; j < node.numChildren(); j++) {
			ParsedNode childOfCompound = (ParsedNode) node.getChild(j);
			jsonArray.addIfNotNull(serializeSingle(childOfCompound));
		}
		return jsonArray;
	}
}
