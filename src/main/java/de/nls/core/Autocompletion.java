package de.nls.core;

import de.nls.ParsedNode;
import de.nls.ebnf.Rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Autocompletion {

	public final String symbolName;

	public final Symbol forSymbol;

	// TODO maybe a Named<Symbol>?

	private String alreadyEntered;

	public Autocompletion(DefaultParsedNode pn) {
		this.symbolName = pn.getName();
		this.forSymbol = pn.getSymbol();
	}

	public Autocompletion(Symbol forSymbol, String symbolName) {
		this.forSymbol = forSymbol;
		this.symbolName = symbolName;
	}

	public static Autocompletion[] literal(DefaultParsedNode pn, String... literal) {
		return Arrays.stream(literal)
				.map(s -> new Autocompletion.Literal(pn, s))
				.toArray(Autocompletion[]::new);
	}

	public static Autocompletion[] literal(DefaultParsedNode pn, List<String> literals) {
		return literals.stream()
				.map(s -> new Autocompletion.Literal(pn, s))
				.toArray(Autocompletion[]::new);
	}

	public static Autocompletion[] literal(DefaultParsedNode pn, List<String> literals, String prefix, String suffix) {
		return literals.stream()
				.map(s -> new Autocompletion.Literal(pn, prefix + s + suffix))
				.toArray(Autocompletion[]::new);
	}

	public static Autocompletion[] parameterized(DefaultParsedNode pn, String parameterName) {
		return new Autocompletion.Parameterized(pn, parameterName).asArray();
	}

	public static Autocompletion[] veto(DefaultParsedNode pn) {
		return new Autocompletion.Veto(pn).asArray();
	}

	public static Autocompletion[] doesAutocomplete(ParsedNode pn) {
		return new Autocompletion.DoesAutocomplete(pn).asArray();
	}

	public abstract String getCompletion();

	public String getAlreadyEntered() {
		return alreadyEntered;
	}

	public void setAlreadyEntered(String alreadyEntered) {
		this.alreadyEntered = alreadyEntered;
	}

	public Autocompletion[] asArray() {
		return new Autocompletion[] { this };
	}

	public static class Literal extends Autocompletion {

		private final String literal;

		private Literal(DefaultParsedNode pn, String s) {
			super(pn);
			this.literal = s;
		}

		private Literal(Symbol forSymbol, String symbolName, String s) {
			super(forSymbol, symbolName);
			this.literal = s;
		}

		@Override
		public String getCompletion() {
			return literal;
		}
	}

	public static class Parameterized extends Autocompletion {
		public final String paramName;

		private Parameterized(DefaultParsedNode pn, String paramName) {
			super(pn);
			this.paramName = paramName;
		}

		public Parameterized(Symbol forSymbol, String symbolName, String paramName) {
			super(forSymbol, symbolName);
			this.paramName = paramName;
		}

		@Override
		public String getCompletion() {
			return "${" + paramName + "}";
		}
	}

	public static class Veto extends Autocompletion {

		public static final String VETO = "VETO";

		private Veto(DefaultParsedNode pn) {
			super(pn);
		}

		private Veto(Symbol forSymbol, String symbolName) {
			super(forSymbol, symbolName);
		}

		@Override
		public String getCompletion() {
			return VETO;
		}
	}

	public static class DoesAutocomplete extends Autocompletion {
		private DoesAutocomplete(ParsedNode pn) {
			super(pn);
		}

		private DoesAutocomplete(Symbol forSymbol, String symbolName) {
			super(forSymbol, symbolName);
		}

		@Override
		public String getCompletion() {
			return "Something"; // the return value for DoesAutocomplete shouldn't matter
		}
	}

	public static class EntireSequence extends Autocompletion {
		private final List<List<Autocompletion>> sequenceOfCompletions = new ArrayList<>();

		private final Rule sequence;

		public EntireSequence(DefaultParsedNode pn) {
			super(pn);
			this.sequence = ((ParsedNode) pn).getRule();
		}

		public EntireSequence(Symbol forSymbol, String symbolName, Rule sequence) {
			super(forSymbol, symbolName);
			this.sequence = sequence;
		}

		public void add(List<Autocompletion> completions) {
			sequenceOfCompletions.add(completions);
		}

		public List<List<Autocompletion>> getSequenceOfCompletions() {
			return sequenceOfCompletions;
		}

		public Rule getSequence() {
			return sequence;
		}

		public void add(Autocompletion completion) {
			ArrayList<Autocompletion> completions = new ArrayList<>();
			completions.add(completion);
			add(completions);
		}

		public void addLiteral(Symbol symbol, String name, String completion) {
			add(new Autocompletion.Literal(symbol, name, completion));
		}

		public void addParameterized(Symbol symbol, String name, String parameter) {
			add(new Autocompletion.Parameterized(symbol, name, parameter));
		}

		@Override
		public String getCompletion() {
			StringBuilder autocompletionString = new StringBuilder();
			int i = 0;
			for(List<Autocompletion> autocompletions : sequenceOfCompletions) {
				int n = autocompletions.size();
				if(n > 1)
					autocompletionString.append("${" + sequence.getNameForChild(i) + "}");
				else if(n == 1)
					autocompletionString.append(autocompletions.get(0).getCompletion());
				i++;
			}
			return autocompletionString.toString();
		}
	}
}
