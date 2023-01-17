package de.nls.ebnf;

import de.nls.Autocompleter;
import de.nls.util.Range;

import static de.nls.ebnf.Named.n;
import static de.nls.core.Terminal.*;

public class EBNF extends EBNFCore {

	private static final String SIGN_NAME            = "sign";
	private static final String INTEGER_NAME         = "int";
	private static final String FLOAT_NAME           = "float";
	private static final String WHITESPACE_STAR_NAME = "whitespace-star";
	private static final String WHITESPACE_PLUS_NAME = "whitespace-plus";
	private static final String INTEGER_RANGE_NAME   = "integer-range";

	public final Rule SIGN;
	public final Rule INTEGER;
	public final Rule FLOAT;
	public final Rule WHITESPACE_STAR;
	public final Rule WHITESPACE_PLUS;
	public final Rule INTEGER_RANGE;

	public EBNF() {
		SIGN            = makeSign();
		INTEGER         = makeInteger();
		FLOAT           = makeFloat();
		WHITESPACE_STAR = makeWhitespaceStar();
		WHITESPACE_PLUS = makeWhitespacePlus();
		INTEGER_RANGE   = makeIntegerRange();
	}

	private Rule makeSign() {
		return or(SIGN_NAME, n(literal("-")), n(literal("+")));
	}

	private Rule makeInteger() {
		// int -> (-|+)?digit+
		Rule ret = sequence(INTEGER_NAME,
				n("optional", optional(null, n("sign", SIGN))),
				n("plus", plus(null, n(DIGIT))));
		ret.setEvaluator(pn -> Integer.parseInt(pn.getParsedString()));
		ret.setAutocompleter(Autocompleter.DEFAULT_INLINE_AUTOCOMPLETER);
		return ret;
	}

	private Rule makeFloat() {
		// float -> (-|+)?digit+(.digit*)?
		Rule ret = sequence(FLOAT_NAME,
				n("", optional(null, n("", SIGN))),
				n("", plus(null, n(DIGIT))),
				n("", optional(null,
						n("sequence", sequence(null,
								n(literal(".")),
								n("star", star(null, n(DIGIT)))
						))
				))
		);
		ret.setEvaluator(pn -> Double.parseDouble(pn.getParsedString()));
		ret.setAutocompleter(Autocompleter.DEFAULT_INLINE_AUTOCOMPLETER);
		return ret;
	}

	private Rule makeWhitespaceStar() {
		Rule ret = star(WHITESPACE_STAR_NAME, n(WHITESPACE));
		ret.setAutocompleter(new Autocompleter.IfNothingYetEnteredAutocompleter(" "));
		return ret;
	}

	private Rule makeWhitespacePlus() {
		Rule ret = plus(WHITESPACE_PLUS_NAME, n(WHITESPACE)).setAutocompleter(pn -> pn.getParsedString().isEmpty() ? " " : "");
		ret.setAutocompleter(new Autocompleter.IfNothingYetEnteredAutocompleter(" "));
		return ret;
	}

	private Rule makeIntegerRange() {
		Rule delimiter = sequence(null,
				n("ws*", WHITESPACE_STAR),
				n(literal("-")),
				n("ws*", WHITESPACE_STAR));
		Rule ret = join(INTEGER_RANGE_NAME,
				n("", INTEGER),
				null,
				null,
				delimiter.getTarget(),
				"from", "to");
		ret.setEvaluator(pn -> new Range(
				(Integer) pn.evaluate(0),
				(Integer) pn.evaluate(1)));
		return ret;
		// TODO set autocompleter
	}
}
