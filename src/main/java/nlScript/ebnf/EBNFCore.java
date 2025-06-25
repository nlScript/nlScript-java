package nlScript.ebnf;

import nlScript.Evaluator;
import nlScript.core.Autocompletion;
import nlScript.core.BNF;
import nlScript.core.Generation;
import nlScript.core.Named;
import nlScript.core.NonTerminal;
import nlScript.core.Production;
import nlScript.core.Symbol;
import nlScript.core.Terminal;
import nlScript.util.Range;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class EBNFCore {

	protected final HashMap<String, Symbol> symbols = new HashMap<>();

	private final ArrayList<Rule> rules = new ArrayList<>();

	private final BNF bnf = new BNF();

	public EBNFCore() {}

	public EBNFCore(EBNFCore other) {
		symbols.putAll(other.symbols);
		rules.addAll(other.rules);
	}

	public Symbol getSymbol(String type) {
		return symbols.get(type);
	}

	public void compile(Symbol topLevelSymbol) {
		// update the start symbol
		removeRules(BNF.ARTIFICIAL_START_SYMBOL);
		Sequence sequence = new Sequence(
				BNF.ARTIFICIAL_START_SYMBOL,
				new Named<Symbol>(topLevelSymbol, "top-level"),
				BNF.ARTIFICIAL_STOP_SYMBOL.withName("Stop"));
		addRule(sequence);
		sequence.setEvaluator(Evaluator.FIRST_CHILD_EVALUATOR);
	}

	public BNF getBNF() {
		return bnf;
	}

	public ArrayList<Rule> getRules(NonTerminal target) {
		ArrayList<Rule> ret = new ArrayList<>(rules);
		ret.removeIf(p -> !p.getTarget().equals(target));
		return ret;
	}

	public Rule plus(String type, Named<?> child) {
		NonTerminal tgt = newOrExistingNonTerminal(type);
		Plus plus = new Plus(tgt, child);
		addRule(plus);
		return plus;
	}

	public Rule star(String type, Named<?> child) {
		NonTerminal tgt = newOrExistingNonTerminal(type);
		Star star = new Star(tgt, child);
		addRule(star);
		return star;
	}

	public Rule or(String type, Named<?>... options) {
		NonTerminal tgt = newOrExistingNonTerminal(type);
		Or or = new Or(tgt, options);
		addRule(or);
		return or;
	}

	public Rule optional(String type, Named<?> child) {
		NonTerminal tgt = newOrExistingNonTerminal(type);
		Optional optional = new Optional(tgt, child);
		addRule(optional);
		return optional;
	}

	public Rule repeat(String type, Named<?> child, int from, int to) {
		NonTerminal tgt = newOrExistingNonTerminal(type);
		Repeat repeat = new Repeat(tgt, child, from, to);
		addRule(repeat);
		return repeat;
	}

	public Rule repeat(String type, Named<?> child, String... names) {
		NonTerminal tgt = newOrExistingNonTerminal(type);
		int n = names.length;
		Repeat repeat = new Repeat(tgt, child, n, n);
		repeat.setParsedChildNames(names);
		addRule(repeat);
		return repeat;
	}

	public Rule join(String type, Named<?> child, Named<?> open, Named<?> close, Named<?> delimiter, Range cardinality) {
		return join(type, child, open, close, delimiter, true, cardinality);
	}

	public Rule join(String type, Named<?> child, Named<?> open, Named<?> close, Named<?> delimiter, boolean onlyKeepEntries, Range cardinality) {
		NonTerminal tgt = newOrExistingNonTerminal(type);
		Join join = new Join(tgt, child, open, close, delimiter, cardinality);
		join.setParsedChildNames(child.getName());
		join.setOnlyKeepEntries(onlyKeepEntries);
		addRule(join);
		return join;
	}

	public Rule join(String type, Named<?> child, Named<?> open, Named<?> close, Named<?> delimiter, String... names) {
		return join(type, child, open, close, delimiter, true, names);
	}

	public Rule join(String type, Named<?> child, Named<?> open, Named<?> close, Named<?> delimiter, boolean onlyKeepEntries, String... names) {
		NonTerminal tgt = newOrExistingNonTerminal(type);
		int n = names.length;
		Join join = new Join(tgt, child, open, close, delimiter, new Range(n, n));
		join.setOnlyKeepEntries(onlyKeepEntries);
		join.setParsedChildNames(names);
		addRule(join);
		return join;
	}

	public Rule list(String type, Named<?> child) {
		NamedRule wsStar = star(null, Terminal.WHITESPACE.withName()).withName("ws*");
		Rule delimiter = sequence(null,
				wsStar,
				Terminal.literal(",").withName(),
				wsStar);
		delimiter.setAutocompleter((pn, justCheck) ->
				Autocompletion.literal(pn, pn.getParsedString().isEmpty() ? ", " : ""));

		return join(type, child, null, null, delimiter.withName("delimiter"), Range.STAR);
	}

	public Rule tuple(String type, Named<?> child, String... names) {
		NamedRule wsStar = star(null, Terminal.WHITESPACE.withName()).withName("ws*");
		wsStar.get().setAutocompleter((pn, justCheck) -> Autocompletion.literal(pn, ""));
		Rule open      = sequence(null, Terminal.literal("(").withName("open"), wsStar);
		Rule close     = sequence(null, wsStar, Terminal.literal(")").withName("close"));
		Rule delimiter = sequence(null, wsStar, Terminal.literal(",").withName("delimiter"), wsStar);
		open     .setGenerator((grammar, hints) -> new Generation("("));
		close    .setGenerator((grammar, hints) -> new Generation(")"));
		delimiter.setGenerator((grammar, hints) -> new Generation(", "));

		Rule ret = join(type, child, open.withName("open"), close.withName("close"), delimiter.withName("delimiter"), names);
		ret.setAutocompleter((pn, justCheck) -> {
			if (!pn.getParsedString().isEmpty())
				return null;
			if (justCheck)
				return Autocompletion.doesAutocomplete(pn);

			Autocompletion.EntireSequence seq = new Autocompletion.EntireSequence(pn);
			seq.addLiteral(open.tgt, "open", "(");
			seq.addParameterized(child.getSymbol(), names[0], names[0]);
			for (int i = 1; i < names.length; i++) {
				seq.addLiteral(delimiter.tgt, "delimiter", ", ");
				seq.addParameterized(child.getSymbol(), names[i], names[i]);
			}
			seq.addLiteral(close.tgt, "close", ")");
			return seq.asArray();
		});
		return ret;
	}

	public Rule sequence(String type, Named<?>... children) {
		NonTerminal tgt = newOrExistingNonTerminal(type);
		Sequence sequence = new Sequence(tgt, children);
		addRule(sequence);
		return sequence;
	}

	private void addRule(Rule rule) {
		if(!symbols.containsKey(rule.tgt.getSymbol()))
			symbols.put(rule.tgt.getSymbol(), rule.tgt);

		for(Named<?> n : rule.children) {
			Symbol s = n.getSymbol();
			if(!s.isEpsilon() && !symbols.containsKey(s.getSymbol()))
				symbols.put(s.getSymbol(), s);
		}
		rules.add(rule);
		rule.createBNF(bnf);
	}

	public void removeRules(NonTerminal symbol) {
		Set<Production> toRemove = new HashSet<>();
		for(int i = rules.size() - 1; i >= 0; i--) {
			if (rules.get(i).tgt.equals(symbol)) {
				Rule rule = rules.remove(i);
				for(Production production : rule.productions)
					toRemove.add(production);
			}
		}
		bnf.removeProductions(toRemove);
	}

	private NonTerminal newOrExistingNonTerminal(String type) {
		if(type == null)
			return null;
		Symbol s = symbols.get(type);
		if(s == null)
			s = new NonTerminal(type);
		return (NonTerminal) s;
	}
}
