package de.nls;

public interface Autocompleter {

	String VETO = "VETO";

	String getAutocompletion(ParsedNode pn);

	class IfNothingYetEnteredAutocompleter implements Autocompleter {

		private final String completion;

		public IfNothingYetEnteredAutocompleter(String completion) {
			this.completion = completion;
		}

		@Override
		public String getAutocompletion(ParsedNode pn) {
			return pn.getParsedString().isEmpty() ? completion : "";
		}
	}

	Autocompleter DEFAULT_INLINE_AUTOCOMPLETER = pn -> {
		String alreadyEntered = pn.getParsedString();
		if(alreadyEntered.length() > 0)
			return Autocompleter.VETO;
		String name = pn.getName();
		if(name != null)
			return "${" + name + "}";
		name = pn.getSymbol().getSymbol();
		if(name != null)
			return "${" + name + "}";
		return null;
	};

}
