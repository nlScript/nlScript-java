package de.nls;

import de.nls.core.Autocompletion;
import de.nls.core.BNF;
import de.nls.core.Lexer;
import de.nls.core.NonTerminal;
import de.nls.core.Production;
import de.nls.core.RDParser;
import de.nls.core.Symbol;
import de.nls.ebnf.EBNFCore;
import de.nls.ebnf.EBNFParsedNodeFactory;
import de.nls.ebnf.Named;
import de.nls.ebnf.Rule;
import de.nls.ebnf.Sequence;

import java.util.ArrayList;

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

	class EntireSequenceCompleter implements Autocompleter {

		private final EBNFCore ebnf;

		public EntireSequenceCompleter(EBNFCore ebnf) {
			this.ebnf = ebnf;
		}

		@Override
		public String getAutocompletion(ParsedNode pn) {
			String alreadyEntered = pn.getParsedString();
			if (alreadyEntered.length() > 0)
				return null;
			StringBuilder autocompletionString = new StringBuilder();

			Rule sequence = pn.getRule();
			Symbol[] children = sequence.getChildren();


			for(int i = 0; i < children.length; i++) {
				BNF bnf = new BNF(ebnf.getBNF());

				Sequence newSequence = new Sequence(null, children[i]);
				newSequence.setParsedChildNames(sequence.getNameForChild(i));
				newSequence.createBNF(bnf);

				bnf.removeStartProduction();
				bnf.addProduction(new Production(BNF.ARTIFICIAL_START_SYMBOL, newSequence.getTarget()));
				RDParser parser = new RDParser(bnf, new Lexer(""), EBNFParsedNodeFactory.INSTANCE);

				ArrayList<Autocompletion> autocompletions = new ArrayList<>();
				parser.parse(autocompletions);

				int n = autocompletions.size();
				if (n > 1)
					autocompletionString.append("${").append(sequence.getNameForChild(i)).append('}');
				else if (n == 1)
					autocompletionString.append(autocompletions.get(0).getCompletion());
			}

//			for (int i = 0; i < children.length; i++) {
//				EBNFCore ebnfCopy = new EBNFCore(ebnf);
//				Rule newSequence = ebnfCopy.sequence(null, Named.n(sequence.getNameForChild(i), children[i]));
//				ebnfCopy.setWhatToMatch(newSequence.getTarget());
//				RDParser parser = new RDParser(ebnfCopy.createBNF(), new Lexer(""), EBNFParsedNodeFactory.INSTANCE);
//				ArrayList<Autocompletion> autocompletions = new ArrayList<>();
//				parser.parse(autocompletions);
//
//				int n = autocompletions.size();
//				if (n > 1)
//					autocompletionString.append("${").append(sequence.getNameForChild(i)).append('}');
//				else if (n == 1)
//					autocompletionString.append(autocompletions.get(0).getCompletion());
//			}
			return autocompletionString.toString();
		}
	}
}
