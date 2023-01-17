package de.nls;

import de.nls.core.Autocompletion;
import de.nls.core.GraphViz;
import de.nls.core.Lexer;
import de.nls.core.NonTerminal;
import de.nls.core.ParsingState;
import de.nls.core.RDParser;
import de.nls.core.Symbol;
import de.nls.core.Terminal;
import de.nls.ebnf.EBNF;
import de.nls.ebnf.Named;
import de.nls.ebnf.Rule;
import de.nls.util.Range;

import java.util.ArrayList;

import static de.nls.ebnf.Named.n;

public class Parser {
	private final EBNF grammar = new EBNF();

	private final Terminal LINEBREAK = Terminal.literal("\n");

	public final Rule QUANTIFIER;
	public final Rule IDENTIFIER;
	public final Rule VARIABLE_NAME;
	public final Rule ENTRY_NAME;
	public final Rule LIST;
	public final Rule TUPLE;
	public final Rule CHARACTER_CLASS;
	public final Rule TYPE;
	public final Rule VARIABLE;
	public final Rule NO_VARIABLE;
	public final Rule EXPRESSION;
//	public final NonTerminal SENTENCE = new NonTerminal("sentence");

	private final Rule LINEBREAK_STAR;

	private final EBNF targetGrammar = new EBNF();


	public Parser() {
		QUANTIFIER      = quantifier();
		IDENTIFIER      = identifier("identifier");
		VARIABLE_NAME   = variableName();
		ENTRY_NAME      = entryName();
		LIST            = list();
		TUPLE           = tuple();
		CHARACTER_CLASS = characterClass();
		TYPE            = type();
		VARIABLE        = variable();
		NO_VARIABLE     = noVariable();
		EXPRESSION      = expression();

		LINEBREAK_STAR = targetGrammar.star("linebreak-star", n(LINEBREAK));
		program();

	}

	public EBNF getGrammar() {
		return grammar;
	}

	public EBNF getTargetGrammar() {
		return targetGrammar;
	}

	public Named.NamedRule defineSentence(String pattern, Evaluator evaluator) {
		return defineSentence(pattern, evaluator, null);
	}

	public Named.NamedRule defineSentence(String pattern, Evaluator evaluator, boolean completeEntireSequence) {
		return defineSentence(pattern, evaluator, null);
	}
	public Named.NamedRule defineSentence(String pattern, Evaluator evaluator, Autocompleter autocompleter) {
		return defineType("sentence", pattern, evaluator, autocompleter);
	}

	public Named.NamedRule defineType(String type, String pattern, Evaluator evaluator) {
		return defineType(type, pattern, evaluator, null);
	}

	public Named.NamedRule defineType(String type, String pattern, Evaluator evaluator, boolean completeEntireSequence) {
		return defineType(type, pattern, evaluator, null);
	}

	public Named.NamedRule defineType(String type, String pattern, Evaluator evaluator, Autocompleter autocompleter) {
		grammar.setWhatToMatch(EXPRESSION.getTarget());
		RDParser parser = new RDParser(grammar.createBNF(), new Lexer(pattern));
		ParsedNode pn = parser.parse();
		if(pn.getMatcher().state != ParsingState.SUCCESSFUL)
			throw new RuntimeException("Parsing failed");
		pn = parser.buildAst(pn);
		System.out.println(GraphViz.toVizDotLink(pn));
		Named[] rhs = (Named[]) pn.evaluate();

		Rule newRule = targetGrammar.sequence(type, rhs);
		if(evaluator != null)
			newRule.setEvaluator(evaluator);
		if(autocompleter != null)
			newRule.setAutocompleter(autocompleter);

		return n(type, newRule);
	}

	public ParsedNode parse(String text, ArrayList<Autocompletion> autocompletions) {
		targetGrammar.setWhatToMatch((NonTerminal) targetGrammar.getSymbol("program"));
		RDParser rdParser = new RDParser(targetGrammar.createBNF(), new Lexer(text));
		fireParsingStarted();
		ParsedNode pn = rdParser.parse(autocompletions);
		if(pn.getMatcher().state == ParsingState.SUCCESSFUL)
			pn = rdParser.buildAst(pn);
		System.out.println(GraphViz.toVizDotLink(pn));
		return pn;
	}

	private Rule quantifier() {
		return grammar.or("quantifier",
				n("optional", grammar.sequence(null, n(         Terminal.literal("?"))).setEvaluator(pn -> Range.OPTIONAL)),
				n("plus",     grammar.sequence(null, n(         Terminal.literal("+"))).setEvaluator(pn -> Range.PLUS)),
				n("star",     grammar.sequence(null, n(         Terminal.literal("*"))).setEvaluator(pn -> Range.STAR)),
				n("range",    grammar.sequence(null, n("range", grammar.INTEGER_RANGE)).setEvaluator(pn -> pn.evaluate(0))),
				n("fixed",    grammar.sequence(null, n("int",   grammar.INTEGER))      .setEvaluator(pn -> new Range((int)pn.evaluate(0))))
		);
	}

	/**
	 * [A-Za-z_] ([A-Za-z0-9-_]* [A-Za-z0-9_])?
	 *
	 * Start:  letter or underscore
	 * Middle: letter or underscore or dash or digit
	 * End:    letter or underscore or digit
	 *
	 */
	private Rule identifier(String name) {
		if(name == null)
			name = "identifier";
		return grammar.sequence(name,
				n(Terminal.characterClass("[A-Za-z_]")),
				n("opt", grammar.optional(null,
						n("seq", grammar.sequence(null,
								n("star", grammar.star(null,
										n(Terminal.characterClass("[A-Za-z0-9_-]"))
								)),
								n(Terminal.characterClass("[A-Za-z0-9_]"))
						))
				))
		);
	}

	/**
	 * (was: ExtendedName)
	 *
	 * [^:{}\n]+
	 *
	 * Everything but ':', '{', '}', '\n'
	 */
	private Rule variableName() {
		return grammar.plus("var-name",
				n(Terminal.characterClass("[^:{}\n]"))).setEvaluator(ParsedNode::getParsedString);
	}

	/**
	 * (was: Name)
	 */
	private Rule entryName() {
		return identifier("entry-name");
	}

	private Rule list() {
		return grammar.sequence("list",
				n(Terminal.literal("list")),
				n("ws*", grammar.WHITESPACE_STAR),
				n(Terminal.literal("<")),
				n("ws*", grammar.WHITESPACE_STAR),
				n("type", IDENTIFIER),
				n("ws*", grammar.WHITESPACE_STAR),
				n(Terminal.literal(">"))
		).setEvaluator(pn -> {
			String identifier = (String) pn.evaluate("type");
			Symbol entry = targetGrammar.getSymbol(identifier);

			Named namedEntry = (entry instanceof Terminal)
					? n((Terminal) entry)
					: n(null, (NonTerminal) entry);
			return targetGrammar.list(null, namedEntry).getTarget();
		});
	}

	private Rule tuple() {
		return grammar.sequence("tuple",
				n(Terminal.literal("tuple")),
				n("ws*", grammar.WHITESPACE_STAR),
				n(Terminal.literal("<")),
				n("ws*", grammar.WHITESPACE_STAR),
				n("type", IDENTIFIER),
				n("plus-names", grammar.plus(null,
						n("sequence-names", grammar.sequence(null,
								n("ws*", grammar.WHITESPACE_STAR),
								n(Terminal.literal(",")),
								n("ws*", grammar.WHITESPACE_STAR),
								n("entry-name", ENTRY_NAME),
								n("ws*", grammar.WHITESPACE_STAR)
						))
				)),
				n(Terminal.literal(">"))
		).setEvaluator(pn -> {
			String type = (String)pn.evaluate("type");
			ParsedNode plus = pn.getChild("plus-names");
			int nTuple = plus.numChildren();
			String[] entryNames = new String[nTuple];
			for(int i = 0; i < nTuple; i++)
				entryNames[i] = (String) plus.getChild(i).evaluate("entry-name");

			Symbol entry = targetGrammar.getSymbol(type);
			Named namedEntry = (entry instanceof Terminal)
					? n((Terminal) entry)
					: n(null, (NonTerminal) entry);

			return targetGrammar.tuple(null, namedEntry, entryNames).getTarget();
		});
	}

	private Rule characterClass() {
		return grammar.sequence("character-class",
				n(Terminal.literal("[")),
				n("plus", grammar.plus(null, n(Terminal.characterClass("[^]]")))),
				n(Terminal.literal("]"))
		).setEvaluator(pn -> {
			String pattern = pn.getParsedString();
			return Terminal.characterClass(pattern);
		});
	}

	private Rule type() {
		return grammar.or("type",
				n("type", grammar.sequence(null, n("identifier", IDENTIFIER)).setEvaluator(pn -> targetGrammar.getSymbol(pn.getParsedString()))),
				n("list", LIST),
				n("tuple", TUPLE),
				n("character-class", CHARACTER_CLASS)
		);
	}

	/*
	 * {name:[:type][:quantifier]}
	 * - either just the name: {From frame}
	 * - or name and type: {frame:int}
	 */
	private Rule variable() {
		return grammar.sequence("variable",
				n(Terminal.literal("{")),
				n("variable-name", VARIABLE_NAME),
				n("opt-type", grammar.optional(null,
						n("seq-type", grammar.sequence(null,
								n(Terminal.literal(":")),
								n("type", TYPE)
						))
				)),
				n("opt-quantifier", grammar.optional(null,
						n("seq-quantifier", grammar.sequence(null,
								n(Terminal.literal(":")),
								n("quantifier", QUANTIFIER)
						))
				)),
				n(Terminal.literal("}"))
		).setEvaluator(pn -> {
			String variableName = (String) pn.evaluate("variable-name");
			Object typeObject = pn.evaluate("opt-type", "seq-type", "type");
			Symbol symbol = typeObject == null
					? Terminal.literal(variableName)
					: (Symbol) typeObject;
			Named namedSymbol = (symbol instanceof Terminal)
					? n((Terminal) symbol)
					: n(variableName, (NonTerminal) symbol);

			Object quantifierObject = pn.evaluate("opt-quantifier", "seq-quantifier", "quantifier");
			if(quantifierObject != null) {
				Range range = (Range) quantifierObject;
				     if(range.equals(Range.STAR))     symbol = targetGrammar.star(    null, namedSymbol).getTarget();
				else if(range.equals(Range.PLUS))     symbol = targetGrammar.plus(    null, namedSymbol).getTarget();
				else if(range.equals(Range.OPTIONAL)) symbol = targetGrammar.optional(null, namedSymbol).getTarget();
				else                                  symbol = targetGrammar.repeat(  null, namedSymbol, range.getLower(), range.getUpper()).getTarget();
				namedSymbol = n(variableName, (NonTerminal) symbol);
			}
			return namedSymbol;
		});
	}

	private Rule noVariable() {
		return grammar.sequence("no-variable",
				n(Terminal.characterClass("[^ \t\n{]")),
				n("tail", grammar.optional(null,
						n("seq", grammar.sequence(null,
								n("middle", grammar.star(null,
										n(Terminal.characterClass("[^{\n]"))
								)),
								n(Terminal.characterClass("[^ \t\n{]"))
						))
				))
		).setEvaluator(pn -> n(Terminal.literal(pn.getParsedString())));
	}

	private Rule expression() {
		return grammar.join("expression",
				n("or", grammar.or(null,
						n("no-variable", NO_VARIABLE),
						n("variable", VARIABLE)
				)),
				null,
				null,
				grammar.WHITESPACE_STAR.getTarget(),
				false,
				Range.PLUS
		).setEvaluator(parsedNode -> {
			int nChildren = parsedNode.numChildren();

			ArrayList<Named> rhsList = new ArrayList<>();

			rhsList.add((Named) parsedNode.getChild(0).evaluate());
			for(int i = 1; i < nChildren; i++) {
				ParsedNode child = parsedNode.getChild(i);
				if(i % 2 == 0) { // or
					rhsList.add((Named) child.evaluate());
				}
				else { // ws*
					boolean hasWS = child.numChildren() > 0;
					if(hasWS)
						rhsList.add(n("ws+", targetGrammar.WHITESPACE_PLUS));
				}
			}
			Named[] rhs = new Named[rhsList.size()];
			rhsList.toArray(rhs);
			return rhs;
		});
	}

	public Rule program() {
		return targetGrammar.join("program",
//				n("sentence", (NonTerminal) targetGrammar.getSymbol("sentence")),
				n("sentence", new NonTerminal("sentence")),
				LINEBREAK_STAR.getTarget(),
				LINEBREAK_STAR.getTarget(),
				LINEBREAK_STAR.getTarget(),
				Range.STAR);
	}

	public interface ParseStartListener {
		void parsingStarted();
	}

	private final ArrayList<ParseStartListener> parseStartListeners = new ArrayList<>();

	public void addParseStartListener(ParseStartListener l) {
		parseStartListeners.add(l);
	}

	public void removeParseStartListener(ParseStartListener l) {
		parseStartListeners.remove(l);
	}

	private void fireParsingStarted() {
		for(ParseStartListener l : parseStartListeners)
			l.parsingStarted();
	}
}
