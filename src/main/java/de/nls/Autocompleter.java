package de.nls;

import de.nls.core.BNF;
import de.nls.core.Lexer;
import de.nls.core.Autocompletion;
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

	Autocompletion[] getAutocompletion(ParsedNode pn, boolean justCheck);

	Autocompleter DEFAULT_INLINE_AUTOCOMPLETER = (pn, justCheck) -> {
		String alreadyEntered = pn.getParsedString();
		if(!alreadyEntered.isEmpty())
			return Autocompletion.veto(pn);
		String name = pn.getName();
		if(name == null)
			name = pn.getSymbol().getSymbol();
		if(name == null)
			return null;
		return Autocompletion.parameterized(pn, name);
	};

	Autocompleter PATH_AUTOCOMPLETER = new PathAutocompleter();

	class EntireSequenceCompleter implements Autocompleter {

		private final EBNFCore ebnf;

		private final HashMap<String, ArrayList<Autocompletion>> symbol2Autocompletion;

		public EntireSequenceCompleter(EBNFCore ebnf, HashMap<String, ArrayList<Autocompletion>> symbol2Autocompletion) {
			this.ebnf = ebnf;
			this.symbol2Autocompletion = symbol2Autocompletion;
		}

		@Override
		public Autocompletion[] getAutocompletion(ParsedNode pn, boolean justCheck) {
			String alreadyEntered = pn.getParsedString();

			Rule sequence = pn.getRule();
			Symbol[] children = sequence.getChildren();

			Autocompletion.EntireSequence entireSequenceCompletion = new Autocompletion.EntireSequence(pn);

			for(int i = 0; i < children.length; i++) {
				String key = children[i].getSymbol() + ":" + sequence.getNameForChild(i);
				ArrayList<Autocompletion> autocompletionsForChild = symbol2Autocompletion.get(key);
				if(autocompletionsForChild != null) {
					entireSequenceCompletion.add(autocompletionsForChild);
					continue;
				}

				BNF bnf = new BNF(ebnf.getBNF());

				Sequence newSequence = new Sequence(null, children[i]);
				newSequence.setParsedChildNames(sequence.getNameForChild(i));
				newSequence.createBNF(bnf);

				bnf.removeStartProduction();
				bnf.addProduction(new Production(BNF.ARTIFICIAL_START_SYMBOL, newSequence.getTarget()));
				RDParser parser = new RDParser(bnf, new Lexer(""), EBNFParsedNodeFactory.INSTANCE);

				autocompletionsForChild = new ArrayList<>();
				try {
					parser.parse(autocompletionsForChild);
				} catch (ParseException e) {
					throw new RuntimeException(e);
				}

				symbol2Autocompletion.put(key, autocompletionsForChild);
				entireSequenceCompletion.add(autocompletionsForChild);
			}

			int idx = entireSequenceCompletion.getCompletion().indexOf("${");
			if(idx >= 0 && alreadyEntered.length() > idx)
				return null;

			return entireSequenceCompletion.asArray();
		}
	}

	class PathAutocompleter implements Autocompleter {
		public Autocompletion[] getAutocompletion(ParsedNode p, boolean justCheck) {
			if(justCheck)
				return Autocompletion.doesAutocomplete(p);
			String[] completion =  CompletePath.getCompletion(p.getParsedString());
			return Autocompletion.literal(p, completion);
		}
	}
}
