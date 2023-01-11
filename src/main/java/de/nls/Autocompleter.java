package de.nls;

public interface Autocompleter {

	String VETO = "VETO";

	String getAutocompletion(ParsedNode pn);
}
