package nlScript;

import nlScript.core.BNF;
import nlScript.core.Lexer;
import nlScript.core.Production;
import nlScript.core.RDParser;
import nlScript.core.Symbol;
import nlScript.core.Terminal;
import nlScript.core.Autocompletion;
import nlScript.core.DefaultParsedNode;
import nlScript.core.Named;
import nlScript.ebnf.EBNFCore;
import nlScript.ebnf.EBNFParsedNodeFactory;
import nlScript.ebnf.Rule;
import nlScript.ebnf.Sequence;
import nlScript.util.CompletePath;

import java.util.ArrayList;
import java.util.HashMap;

public interface Autocompleter {

	Autocompletion[] getAutocompletion(DefaultParsedNode pn, boolean justCheck);

	Autocompleter FALLBACK_AUTOCOMPLETER = (pn, justCheck) -> {
		Symbol symbol = pn.getSymbol();
		if(symbol == null)
			return null;

		if(symbol instanceof Terminal.Literal)
			return Autocompletion.literal(pn, ((Terminal.Literal)symbol).getLiteral());

		String name = pn.getName();
		if(name.equals(Named.UNNAMED))
			name = symbol.getSymbol();

		if(symbol.isTerminal()) {
			return !pn.getParsedString().isEmpty()
					? Autocompletion.veto(pn)
					: Autocompletion.parameterized(pn, name);
		}

		return null;
	};

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
		public Autocompletion[] getAutocompletion(DefaultParsedNode pn, boolean justCheck) {
			String alreadyEntered = pn.getParsedString();

			Rule sequence = ((ParsedNode) pn).getRule();
			Named<?>[] children = sequence.getChildren();

			Autocompletion.EntireSequence entireSequenceCompletion = new Autocompletion.EntireSequence(pn);

			for (Named<?> child : children) {
				String key = child.getSymbol() + ":" + child.getName();
				ArrayList<Autocompletion> autocompletionsForChild = symbol2Autocompletion.get(key);
				if (autocompletionsForChild != null) {
					entireSequenceCompletion.add(autocompletionsForChild);
					continue;
				}

				BNF bnf = new BNF(ebnf.getBNF());

				Sequence newSequence = new Sequence(null, child);
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

			// avoid to call getCompletion() more often than necessary
			if(alreadyEntered.isEmpty())
				return entireSequenceCompletion.asArray();

			int idx = entireSequenceCompletion.getCompletion(Autocompletion.Purpose.FOR_INSERTION).indexOf("${");
			if(idx >= 0 && alreadyEntered.length() > idx)
				return null;

			return entireSequenceCompletion.asArray();
		}
	}

	class PathAutocompleter implements Autocompleter {
		@Override
		public Autocompletion[] getAutocompletion(DefaultParsedNode p, boolean justCheck) {
			if(justCheck)
				return Autocompletion.doesAutocomplete(p);
			String[] completion =  CompletePath.getCompletion(p.getParsedString());
			return Autocompletion.literal(p, completion);
		}
	}
}
