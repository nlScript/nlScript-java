package de.nls.ebnf;

import de.nls.core.NonTerminal;
import de.nls.core.Symbol;
import de.nls.core.Terminal;

public abstract class Named {

	public static NamedRule n(String name, Rule rule) {
		return new NamedRule(name, rule);
	}

	public static NamedTerminal n(Terminal t) {
		return new NamedTerminal(t.getSymbol(), t);
	}

	public static NamedTerminal n(String name, Terminal t) {
		return new NamedTerminal(name, t);
	}

	public static NamedNonTerminal n(String name, NonTerminal t) {
		return new NamedNonTerminal(name, t);
	}

	public static Named n(String name, Symbol t) {
		if(t.isTerminal())
			return n(name, (Terminal) t);
		else
			return n(name, (NonTerminal) t);
	}



	final String name;

	public Named(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public abstract Symbol getSymbol();


	public static class NamedRule extends Named {
		final Rule rule;

		public NamedRule(String name, Rule rule) {
			super(name);
			this.rule = rule;
		}

		public void onSuccessfulParsed(ParseListener listener) {
			rule.onSuccessfulParsed(listener);
		}

		@Override
		public Symbol getSymbol() {
			return getTarget();
		}

		public NonTerminal getTarget() {
			return rule.getTarget();
		}
	}

	public static class NamedTerminal extends Named {
		private final Terminal terminal;

		public NamedTerminal(String name, Terminal terminal) {
			super(name);
			this.terminal = terminal;
		}

		@Override
		public Symbol getSymbol() {
			return getTerminal();
		}

		public Terminal getTerminal() {
			return terminal;
		}
	}

	public static class NamedNonTerminal extends Named {
		private final NonTerminal nonTerminal;

		public NamedNonTerminal(String name, NonTerminal nonTerminal) {
			super(name);
			this.nonTerminal = nonTerminal;
		}

		@Override
		public Symbol getSymbol() {
			return getNonTerminal();
		}

		public NonTerminal getNonTerminal() {
			return nonTerminal;
		}
	}
}
