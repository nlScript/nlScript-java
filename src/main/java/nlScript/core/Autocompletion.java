package nlScript.core;

import nlScript.ParsedNode;
import nlScript.ebnf.Rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Autocompletion {

	public enum Purpose {
		FOR_MENU,
		FOR_INSERTION
	}

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

	public static Autocompletion[] literal(DefaultParsedNode pn, CharSequence... literal) {
		return Arrays.stream(literal)
				.map(s -> new Autocompletion.Literal(pn, s.toString()))
				.toArray(Autocompletion[]::new);
	}

	public static Autocompletion[] literal(Symbol forSymbol, String symbolName, CharSequence... literal) {
		return Arrays.stream(literal)
				.map(s -> new Autocompletion.Literal(forSymbol, symbolName, s.toString()))
				.toArray(Autocompletion[]::new);
	}

	public static <T extends CharSequence> Autocompletion[] literal(DefaultParsedNode pn, List<T> literals) {
		return literals.stream()
				.map(s -> new Autocompletion.Literal(pn, s.toString()))
				.toArray(Autocompletion[]::new);
	}

	public static <T extends CharSequence> Autocompletion[] literal(DefaultParsedNode pn, List<T> literals, String prefix, String suffix) {
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

	public static Autocompletion[] doesAutocomplete(DefaultParsedNode pn) {
		return new Autocompletion.DoesAutocomplete(pn).asArray();
	}

	public abstract String getCompletion(Purpose purpose);

	public boolean isEmptyLiteral() {
		return (this instanceof Literal) && this.getCompletion(Purpose.FOR_INSERTION).isEmpty();
	}

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
		public String getCompletion(Purpose purpose) {
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
		public String getCompletion(Purpose purpose) {
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
		public String getCompletion(Purpose purpose) {
			return VETO;
		}
	}

	public static class DoesAutocomplete extends Autocompletion {
		private DoesAutocomplete(DefaultParsedNode pn) {
			super(pn);
		}

		private DoesAutocomplete(Symbol forSymbol, String symbolName) {
			super(forSymbol, symbolName);
		}

		@Override
		public String getCompletion(Purpose purpose) {
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
		public String getCompletion(Purpose purpose) {
			StringBuilder autocompletionString = new StringBuilder();
			int i = 0;
			for(List<Autocompletion> autocompletions : sequenceOfCompletions) {
				int n = autocompletions.size();
				if(n > 1) {
					autocompletionString.append("${" + sequence.getNameForChild(i) + "}");
				}
				else if(n == 1) {
					if(purpose == Purpose.FOR_MENU) {
						String ins = null;
						Autocompletion ac = autocompletions.get(0);
						if(ac instanceof Literal)
							ins = ac.getCompletion(Purpose.FOR_INSERTION);
						else
							ins = "${" + sequence.getNameForChild(i) + "}";

						if(ins == null || ins.equals(Named.UNNAMED))
							ins = "${" + sequence.getChildren()[i].getSymbol() + "}";


						autocompletionString.append(ins);
					}
					else if(purpose == Purpose.FOR_INSERTION)
						autocompletionString.append(autocompletions.get(0).getCompletion(purpose));

					i++;
				}
			}
			return autocompletionString.toString();
		}
	}
}
