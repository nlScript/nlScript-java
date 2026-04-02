package nlScript;

import nlScript.util.Json;

public interface JsonSerializer {
	Json.JsonThing toJSON(ParsedNode pn);
}
