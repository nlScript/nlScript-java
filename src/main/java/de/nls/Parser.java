package de.nls;

import de.nls.core.*;
import de.nls.ebnf.EBNF;
import de.nls.ebnf.EBNFParsedNodeFactory;
import de.nls.ebnf.EBNFParser;
import de.nls.ebnf.Join;
import de.nls.ebnf.NamedRule;
import de.nls.ebnf.Rule;
import de.nls.util.Range;

import java.util.ArrayList;
import java.util.HashMap;

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

	private final Rule LINEBREAK_STAR;

	private final EBNF targetGrammar = new EBNF();

	private final HashMap<String, ArrayList<Autocompletion>> symbol2Autocompletion = new HashMap<>();

	private boolean compiled = false;

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

		LINEBREAK_STAR = targetGrammar.star("linebreak-star", LINEBREAK.withName());
		program();

	}

	public EBNF getGrammar() {
		return grammar;
	}

	public EBNF getTargetGrammar() {
		return targetGrammar;
	}

	public NamedRule defineSentence(String pattern, Evaluator evaluator) {
		return defineSentence(pattern, evaluator, null);
	}

	public NamedRule defineSentence(String pattern, Evaluator evaluator, boolean completeEntireSequence) {
		Autocompleter autocompleter = completeEntireSequence
				? new Autocompleter.EntireSequenceCompleter(targetGrammar, symbol2Autocompletion)
				: Autocompleter.DEFAULT_INLINE_AUTOCOMPLETER;
		return defineSentence(pattern, evaluator, autocompleter);
	}

	public NamedRule defineSentence(String pattern, Evaluator evaluator, Autocompleter autocompleter) {
		return defineType("sentence", pattern, evaluator, autocompleter);
	}

	public NamedRule defineType(String type, String pattern, Evaluator evaluator) {
		return defineType(type, pattern, evaluator, null);
	}

	public NamedRule defineType(String type, String pattern, Evaluator evaluator, boolean completeEntireSequence) {
		Autocompleter autocompleter = completeEntireSequence
				? new Autocompleter.EntireSequenceCompleter(targetGrammar, symbol2Autocompletion)
				: Autocompleter.DEFAULT_INLINE_AUTOCOMPLETER;
		return defineType(type, pattern, evaluator, autocompleter);
	}

	public NamedRule defineType(String type, String pattern, Evaluator evaluator, Autocompleter autocompleter) {
		grammar.compile(EXPRESSION.getTarget());
		RDParser parser = new RDParser(
				grammar.getBNF(),
				new Lexer(pattern),
				EBNFParsedNodeFactory.INSTANCE);
		DefaultParsedNode pn;
		try {
			pn = parser.parse();
		} catch (ParseException e) {
			throw new RuntimeException("Parsing failed", e);
		}
		if(pn.getMatcher().state != ParsingState.SUCCESSFUL)
			throw new RuntimeException("Parsing failed");
		// pn = parser.buildAst(pn);
//		System.out.println(GraphViz.toVizDotLink(pn));
		Named<?>[] rhs = (Named<?>[]) pn.evaluate();

		Rule newRule = targetGrammar.sequence(type, rhs);
		if(evaluator != null)
			newRule.setEvaluator(evaluator);
		if(autocompleter != null)
			newRule.setAutocompleter(autocompleter);

		return newRule.withName(type);
	}

	public void compile() {
		compile(targetGrammar.getSymbol("program"));
	}

	public void compile(Symbol symbol) {
		targetGrammar.compile(symbol);
		compiled = true;
	}

	public ParsedNode parse(String text, ArrayList<Autocompletion> autocompletions) throws ParseException {
		if(!compiled)
			compile();
		symbol2Autocompletion.clear();
		BNF grammar = targetGrammar.getBNF();
		EBNFParser rdParser = new EBNFParser(grammar, new Lexer(text));
		rdParser.addParseStartListener(this::fireParsingStarted);
		return (ParsedNode) rdParser.parse(autocompletions);
	}

	private Rule quantifier() {
		return grammar.or("quantifier",
				grammar.sequence(null, Terminal.literal("?").withName()).       setEvaluator(pn -> Range.OPTIONAL).withName("optional"),
				grammar.sequence(null, Terminal.literal("+").withName()).       setEvaluator(pn -> Range.PLUS).withName("plus"),
				grammar.sequence(null, Terminal.literal("*").withName()).       setEvaluator(pn -> Range.STAR).withName("star"),
				grammar.sequence(null, grammar.INTEGER_RANGE.withName("range")).setEvaluator(pn -> pn.evaluate(0)).withName("range"),
				grammar.sequence(null,       grammar.INTEGER.withName("int")).  setEvaluator(pn -> new Range((int)pn.evaluate(0))).withName("fixed")
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
				Terminal.characterClass("[A-Za-z_]").withName(),
				grammar.optional(null,
						grammar.sequence(null,
								grammar.star(null,
										Terminal.characterClass("[A-Za-z0-9_-]").withName()
								).withName("star"),
								Terminal.characterClass("[A-Za-z0-9_]").withName()
						).withName("seq")
				).withName("opt")
		);
	}

	/**
	 * (was: ExtendedName)
	 *
	 * [^:{}\n]+
	 *
	 * Everything but ':', '{', '}'
	 */
	private Rule variableName() {
		return grammar.plus("var-name",
				Terminal.characterClass("[^:{}]").withName()).setEvaluator(Evaluator.DEFAULT_EVALUATOR);
	}

	/**
	 * (was: Name)
	 */
	private Rule entryName() {
		return identifier("entry-name");
	}

	// evaluates to the target grammar's list rule (i.e. Join).
	private Rule list() {
		return grammar.sequence("list",
				Terminal.literal("list").withName(),
				grammar.WHITESPACE_STAR.withName("ws*"),
				Terminal.literal("<").withName(),
				grammar.WHITESPACE_STAR.withName("ws*"),
				IDENTIFIER.withName("type"),
				grammar.WHITESPACE_STAR.withName("ws*"),
				Terminal.literal(">").withName()
		).setEvaluator(pn -> {
			String identifier = (String) pn.evaluate("type");
			Symbol entry = targetGrammar.getSymbol(identifier);

			Named<?> namedEntry = (entry instanceof Terminal)
					? ((Terminal) entry).withName(identifier)
					: ((NonTerminal) entry).withName(identifier);
			return targetGrammar.list(null, namedEntry);
		});
	}

	private Rule tuple() {
		return grammar.sequence("tuple",
				Terminal.literal("tuple").withName(),
				grammar.WHITESPACE_STAR.withName("ws*"),
				Terminal.literal("<").withName(),
				grammar.WHITESPACE_STAR.withName("ws*"),
				IDENTIFIER.withName("type"),
				grammar.plus(null,
						grammar.sequence(null,
								grammar.WHITESPACE_STAR.withName("ws*"),
								Terminal.literal(",").withName(),
								grammar.WHITESPACE_STAR.withName("ws*"),
								ENTRY_NAME.withName("entry-name"),
								grammar.WHITESPACE_STAR.withName("ws*")
						).withName("sequence-names")
				).withName("plus-names"),
				Terminal.literal(">").withName()
		).setEvaluator(pn -> {
			String type = (String)pn.evaluate("type");
			DefaultParsedNode plus = pn.getChild("plus-names");
			int nTuple = plus.numChildren();
			String[] entryNames = new String[nTuple];
			for(int i = 0; i < nTuple; i++)
				entryNames[i] = (String) plus.getChild(i).evaluate("entry-name");

			Symbol entry = targetGrammar.getSymbol(type);
			Named<?> namedEntry = (entry instanceof Terminal)
					? ((Terminal) entry).withName()
					: ((NonTerminal) entry).withName();

			return targetGrammar.tuple(null, namedEntry, entryNames).getTarget();
		});
	}

	private Rule characterClass() {
		return grammar.sequence("character-class",
				Terminal.literal("[").withName(),
				grammar.plus(null,
						grammar.or(null,
							Terminal.characterClass("[^]]").withName(),
							Terminal.literal("\\]").withName()
						).withName()
				).withName("plus"),
				Terminal.literal("]").withName()
		).setEvaluator(pn -> {
			String pattern = pn.getParsedString();
			// return Terminal.characterClass(pattern);
			return targetGrammar.makeCharacterClass(null, pattern).getTarget();
		});
	}

	private Rule type() {
		return grammar.or("type",
				grammar.sequence(null,
						IDENTIFIER.withName("identifier")
				).setEvaluator(pn -> {
					String str = pn.getParsedString();
					Symbol symbol = targetGrammar.getSymbol(str);
					if(symbol == null)
						throw new RuntimeException("Unknown type '" + str + "'");
					return symbol;
				}).withName("type"),
				LIST.withName("list"),
				TUPLE.withName("tuple"),
				CHARACTER_CLASS.withName("character-class")
		);
	}

	/*
	 * {name:[:type][:quantifier]}
	 * - either just the name: {From frame}
	 * - or name and type: {frame:int}
	 */
	private Rule variable() {
		return grammar.sequence("variable",
				Terminal.literal("{").withName(),
				VARIABLE_NAME.withName("variable-name"),
				grammar.optional(null,
						grammar.sequence(null,
								Terminal.literal(":").withName(),
								TYPE.withName("type")
						).withName("seq-type")
				).withName("opt-type"),
				grammar.optional(null,
						grammar.sequence(null,
								Terminal.literal(":").withName(),
								QUANTIFIER.withName("quantifier")
						).withName("seq-quantifier")
				).withName("opt-quantifier"),
				Terminal.literal("}").withName()
		).setEvaluator(pn -> {
			String variableName = (String) pn.evaluate("variable-name");
			Object typeObject = pn.evaluate("opt-type", "seq-type", "type");
			Object quantifierObject = pn.evaluate("opt-quantifier", "seq-quantifier", "quantifier");


			// typeObject is either
			// - a type (symbol) from the target grammar, or
			// - a character-class (i.e. a terminal), or
			// - a tuple (i.e. symbol of the tuple in the target grammar), or
			// - a list (i.e. a Rule, or more specifically a Join).
			if(typeObject instanceof Join) {
				Join join = (Join) typeObject;
				if(quantifierObject != null)
					join.setCardinality((Range) quantifierObject);
				return join.getTarget().withName(variableName);
			}

			Symbol symbol = typeObject == null
					? Terminal.literal(variableName)
					: (Symbol) typeObject;

			Named<?> namedSymbol = (symbol instanceof Terminal)
					? ((Terminal) symbol).withName(variableName)
					: ((NonTerminal) symbol).withName(variableName);

			if(quantifierObject != null) {
				Autocompleter autocompleter = null;
				// set a new fallback autocompleter. This is important for e.g. {bla:[a-z]:4} or {bla:digit:4}
				if(typeObject instanceof Terminal)
					autocompleter = Autocompleter.DEFAULT_INLINE_AUTOCOMPLETER;
				Range range = (Range) quantifierObject;
				     if(range.equals(Range.STAR))     symbol = targetGrammar.star(    null, namedSymbol).setAutocompleter(autocompleter).getTarget();
				else if(range.equals(Range.PLUS))     symbol = targetGrammar.plus(    null, namedSymbol).setAutocompleter(autocompleter).getTarget();
				else if(range.equals(Range.OPTIONAL)) symbol = targetGrammar.optional(null, namedSymbol).setAutocompleter(autocompleter).getTarget();
				else                                  symbol = targetGrammar.repeat(  null, namedSymbol, range.getLower(), range.getUpper()).setAutocompleter(autocompleter).getTarget();
				namedSymbol = ((NonTerminal) symbol).withName(variableName);
			}
			return namedSymbol;
		});
	}

	private Rule noVariable() {
		return grammar.sequence("no-variable",
				Terminal.characterClass("[^ \t\n{]").withName(),
				grammar.optional(null,
						grammar.sequence(null,
								grammar.star(null,
										Terminal.characterClass("[^{\n]").withName()
								).withName("middle"),
								Terminal.characterClass("[^ \t\n{]").withName()
						).withName("seq")
				).withName("tail")
		).setEvaluator(pn -> Terminal.literal(pn.getParsedString()).withName());
	}

	private Rule expression() {
		return grammar.join("expression",
				grammar.or(null,
						NO_VARIABLE.withName("no-variable"),
						VARIABLE.withName("variable")
				).withName("or"),
				null,
				null,
				grammar.WHITESPACE_STAR.getTarget(),
				false,
				Range.PLUS
		).setEvaluator(parsedNode -> {
			int nChildren = parsedNode.numChildren();

			ArrayList<Named<?>> rhsList = new ArrayList<>();

			rhsList.add((Named<?>) parsedNode.evaluate(0));
			for(int i = 1; i < nChildren; i++) {
				DefaultParsedNode child = parsedNode.getChild(i);
				if(i % 2 == 0) { // or
					rhsList.add((Named<?>) child.evaluate());
				}
				else { // ws*
					boolean hasWS = child.numChildren() > 0;
					if(hasWS)
						rhsList.add(targetGrammar.WHITESPACE_PLUS.withName("ws+"));
				}
			}
			Named<?>[] rhs = new Named[rhsList.size()];
			rhsList.toArray(rhs);
			return rhs;
		});
	}

	private Rule program() {
		return targetGrammar.join("program",
				new NonTerminal("sentence").withName("sequence"),
				LINEBREAK_STAR.getTarget(),
				LINEBREAK_STAR.getTarget(),
				LINEBREAK_STAR.getTarget(),
				Range.STAR);
	}

	private final ArrayList<EBNFParser.ParseStartListener> parseStartListeners = new ArrayList<>();

	public void addParseStartListener(EBNFParser.ParseStartListener l) {
		parseStartListeners.add(l);
	}

	public void removeParseStartListener(EBNFParser.ParseStartListener l) {
		parseStartListeners.remove(l);
	}

	private void fireParsingStarted() {
		for(EBNFParser.ParseStartListener l : parseStartListeners)
			l.parsingStarted();
	}
}
