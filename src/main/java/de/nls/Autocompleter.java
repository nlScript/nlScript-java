package de.nls;

import de.nls.core.Autocompletion;
import de.nls.core.BNF;
import de.nls.core.Lexer;
import de.nls.core.Production;
import de.nls.core.RDParser;
import de.nls.core.Symbol;
import de.nls.ebnf.EBNFCore;
import de.nls.ebnf.EBNFParsedNodeFactory;
import de.nls.ebnf.Rule;
import de.nls.ebnf.Sequence;
import de.nls.util.CompletePath;

import java.util.ArrayList;
import java.util.HashMap;

public interface Autocompleter {

	String VETO = "VETO";

	String DOES_AUTOCOMPLETE = "DOES_AUTOCOMPLETE";

	String getAutocompletion(ParsedNode pn, boolean justCheck);

	class IfNothingYetEnteredAutocompleter implements Autocompleter {

		private final String completion;

		public IfNothingYetEnteredAutocompleter(String completion) {
			this.completion = completion;
		}

		@Override
		public String getAutocompletion(ParsedNode pn, boolean justCheck) {
			return pn.getParsedString().isEmpty() ? completion : "";
		}
	}

	Autocompleter DEFAULT_INLINE_AUTOCOMPLETER = (pn, justCheck) -> {
		String alreadyEntered = pn.getParsedString();
		if(!alreadyEntered.isEmpty())
			return Autocompleter.VETO;
		String name = pn.getName();
		if(name != null)
			return "${" + name + "}";
		name = pn.getSymbol().getSymbol();
		if(name != null)
			return "${" + name + "}";
		return null;
	};

	Autocompleter PATH_AUTOCOMPLETER = new PathAutocompleter();

	class EntireSequenceCompleter implements Autocompleter {

		private final EBNFCore ebnf;

		private final HashMap<String, String> symbol2Autocompletion;

		public EntireSequenceCompleter(EBNFCore ebnf, HashMap<String, String> symbol2Autocompletion) {
			this.ebnf = ebnf;
			this.symbol2Autocompletion = symbol2Autocompletion;
		}

		@Override
		public String getAutocompletion(ParsedNode pn, boolean justCheck) {
			String alreadyEntered = pn.getParsedString();
//			if (!alreadyEntered.isEmpty())
//				return Autocompleter.VETO;

//			if(justCheck)
//				return Autocompleter.DOES_AUTOCOMPLETE;

			StringBuilder autocompletionString = new StringBuilder();

			Rule sequence = pn.getRule();
			Symbol[] children = sequence.getChildren();


			for(int i = 0; i < children.length; i++) {
				String key = children[i].getSymbol() + ":" + sequence.getNameForChild(i);
				String autocompletionStringForChild = symbol2Autocompletion.get(key);
				if(autocompletionStringForChild != null) {
					autocompletionString.append(autocompletionStringForChild);
					continue;
				}
				BNF bnf = new BNF(ebnf.getBNF());

				Sequence newSequence = new Sequence(null, children[i]);
				newSequence.setParsedChildNames(sequence.getNameForChild(i));
				newSequence.createBNF(bnf);

				bnf.removeStartProduction();
				bnf.addProduction(new Production(BNF.ARTIFICIAL_START_SYMBOL, newSequence.getTarget()));
				RDParser parser = new RDParser(bnf, new Lexer(""), EBNFParsedNodeFactory.INSTANCE);

				ArrayList<Autocompletion> autocompletions = new ArrayList<>();
				try {
					parser.parse(autocompletions);
				} catch (ParseException e) {
					throw new RuntimeException(e);
				}

				int n = autocompletions.size();
				if (n > 1)
					autocompletionStringForChild = "${" + sequence.getNameForChild(i) + "}";
				else if (n == 1)
					autocompletionStringForChild = autocompletions.get(0).getCompletion();

				symbol2Autocompletion.put(key, autocompletionStringForChild);
				autocompletionString.append(autocompletionStringForChild);
			}
			int idx = autocompletionString.indexOf("${");
			if(idx >= 0 && alreadyEntered.length() > idx)
				return null;
			return autocompletionString.toString();
		}
	}

	class PathAutocompleter implements Autocompleter {
		public String getAutocompletion(ParsedNode p, boolean justCheck) {
			if(justCheck)
				return DOES_AUTOCOMPLETE;
			return CompletePath.getCompletion(p.getParsedString());
		}
	}
}
