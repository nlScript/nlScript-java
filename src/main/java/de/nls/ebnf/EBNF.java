package de.nls.ebnf;

import de.nls.Autocompleter;
import de.nls.Evaluator;
import de.nls.core.Terminal;
import de.nls.util.CompletePath;
import de.nls.util.Range;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

import static de.nls.core.Terminal.literal;

public class EBNF extends EBNFCore {

//	public static final String LETTER_NAME          = "letter";
	public static final String SIGN_NAME            = "sign";
	public static final String INTEGER_NAME         = "int";
	public static final String FLOAT_NAME           = "float";
	public static final String WHITESPACE_STAR_NAME = "whitespace-star";
	public static final String WHITESPACE_PLUS_NAME = "whitespace-plus";
	public static final String INTEGER_RANGE_NAME   = "integer-range";
	public static final String PATH_NAME            = "path";
	public static final String TIME_NAME            = "time";
	public static final String COLOR_NAME           = "color";

//	public final Rule LETTER;
	public final Rule SIGN;
	public final Rule INTEGER;
	public final Rule FLOAT;
	public final Rule WHITESPACE_STAR;
	public final Rule WHITESPACE_PLUS;
	public final Rule INTEGER_RANGE;
	public final Rule PATH;
	public final Rule TIME;
	public final Rule COLOR;

	public EBNF() {
//		LETTER          = makeLetter();
		SIGN            = makeSign();
		INTEGER         = makeInteger();
		FLOAT           = makeFloat();
		WHITESPACE_STAR = makeWhitespaceStar();
		WHITESPACE_PLUS = makeWhitespacePlus();
		INTEGER_RANGE   = makeIntegerRange();
		PATH            = makePath();
		TIME            = makeTime();
		COLOR           = makeColor();
	}

	public static void clearFilesystemCache() {
		CompletePath.clearFilesystemCache();
	}

	private Rule makeSign() {
		return or(SIGN_NAME, literal("-").withName(), literal("+").withName());
	}

	private Rule makeInteger() {
		// int -> (-|+)?digit+
		Rule ret = sequence(INTEGER_NAME,
				optional(null, SIGN.withName("sign")).withName("optional"),
				plus(null, Terminal.DIGIT.withName()).withName("plus")
		);
		ret.setEvaluator(pn -> Integer.parseInt(pn.getParsedString()));
		ret.setAutocompleter(Autocompleter.DEFAULT_INLINE_AUTOCOMPLETER);
		return ret;
	}

//	private Rule makeLetter() {
//		Rule ret = sequence(LETTER_NAME, Terminal.LETTER.withName());
//		ret.setEvaluator(pn -> pn.getParsedString().charAt(0));
//		ret.setAutocompleter(Autocompleter.DEFAULT_INLINE_AUTOCOMPLETER);
//		return ret;
//	}

	private Rule makeFloat() {
		// float -> (-|+)?digit+(.digit*)?
		Rule ret = sequence(FLOAT_NAME,
				optional(null, SIGN.withName()).withName(),
				plus(null, Terminal.DIGIT.withName()).withName(),
				optional(null,
						sequence(null,
								literal(".").withName(),
								star(null, Terminal.DIGIT.withName()).withName("star")
						).withName("sequence")
				).withName()
		);
		/*
		 * Here is an idea how this could be written nicer:
		 *
		 * ret = sequence(
		 *           optional(SIGN).withName("opt"),
		 *           plus(DIGIT).withName("plus")
		 *           optional(
		 *               sequence(
		 *                   literal(".").withName(),
		 *                   star(DIGIT).withName()
		 *               ).withName("seq")
		 *           ).withName()
		 *       );
		 *
		 */
		ret.setEvaluator(pn -> Double.parseDouble(pn.getParsedString()));
		ret.setAutocompleter(Autocompleter.DEFAULT_INLINE_AUTOCOMPLETER);
		return ret;
	}

	private Rule makeWhitespaceStar() {
		Rule ret = star(WHITESPACE_STAR_NAME, Terminal.WHITESPACE.withName());
		ret.setAutocompleter(new Autocompleter.IfNothingYetEnteredAutocompleter(" "));
		return ret;
	}

	private Rule makeWhitespacePlus() {
		Rule ret = plus(WHITESPACE_PLUS_NAME, Terminal.WHITESPACE.withName());
		ret.setAutocompleter(new Autocompleter.IfNothingYetEnteredAutocompleter(" "));
		return ret;
	}

	private Rule makeIntegerRange() {
		Rule delimiter = sequence(null,
				WHITESPACE_STAR.withName("ws*"),
				literal("-").withName(),
				WHITESPACE_STAR.withName("ws*"));
		Rule ret = join(INTEGER_RANGE_NAME,
				INTEGER.withName(),
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

	private Rule makeColor() {
		Rule black       = sequence(null, literal("black"       ).withName()).setEvaluator(pn -> rgb2int(  0,   0,   0));
		Rule white       = sequence(null, literal("white"       ).withName()).setEvaluator(pn -> rgb2int(255, 255, 255));
		Rule red         = sequence(null, literal("red"         ).withName()).setEvaluator(pn -> rgb2int(255,   0,   0));
		Rule orange      = sequence(null, literal("orange"      ).withName()).setEvaluator(pn -> rgb2int(255, 128,   0));
		Rule yellow      = sequence(null, literal("yellow"      ).withName()).setEvaluator(pn -> rgb2int(255, 255,   0));
		Rule lawngreen   = sequence(null, literal("lawn green"  ).withName()).setEvaluator(pn -> rgb2int(128, 255,   0));
		Rule green       = sequence(null, literal("green"       ).withName()).setEvaluator(pn -> rgb2int(  0, 255,   0));
		Rule springgreen = sequence(null, literal("spring green").withName()).setEvaluator(pn -> rgb2int(  0, 255, 180));
		Rule cyan        = sequence(null, literal("cyan"        ).withName()).setEvaluator(pn -> rgb2int(  0, 255, 255));
		Rule azure       = sequence(null, literal("azure"       ).withName()).setEvaluator(pn -> rgb2int(  0, 128, 255));
		Rule blue        = sequence(null, literal("blue"        ).withName()).setEvaluator(pn -> rgb2int(  0,   0, 255));
		Rule violet      = sequence(null, literal("violet"      ).withName()).setEvaluator(pn -> rgb2int(128,   0, 255));
		Rule magenta     = sequence(null, literal("magenta"     ).withName()).setEvaluator(pn -> rgb2int(255,   0, 255));
		Rule pink        = sequence(null, literal("pink"        ).withName()).setEvaluator(pn -> rgb2int(255,   0, 128));
		Rule gray        = sequence(null, literal("gray"        ).withName()).setEvaluator(pn -> rgb2int(128, 128, 128));

		Rule custom = tuple(null, INTEGER.withName(), "red", "green", "blue");

		return or(COLOR_NAME,
				custom.withName(),
				black.withName(),
				white.withName(),
				red.withName(),
				orange.withName(),
				yellow.withName(),
				lawngreen.withName(),
				green.withName(),
				springgreen.withName(),
				cyan.withName(),
				azure.withName(),
				blue.withName(),
				violet.withName(),
				magenta.withName(),
				pink.withName(),
				gray.withName());
	}

	private static int rgb2int(int r, int g, int b) {
		return (0xff << 24) | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
	}

	private Rule makeTime() {
		Rule ret = sequence(TIME_NAME,
				optional(null, Terminal.DIGIT.withName()).withName(),
				Terminal.DIGIT.withName(),
				literal(":").withName(),
				Terminal.DIGIT.withName(),
				Terminal.DIGIT.withName());
		ret.setEvaluator(pn -> LocalTime.parse(pn.getParsedString(), DateTimeFormatter.ofPattern("H:mm")));
		ret.setAutocompleter(new Autocompleter.IfNothingYetEnteredAutocompleter("${HH}:${MM}"));
		return ret;
	}

	private Rule makePath() {
		Rule innerPath = plus(null,
				Terminal.characterClass("[^'<>|?*\n]").withName("inner-path"));
		innerPath.setEvaluator(Evaluator.DEFAULT_EVALUATOR);
		innerPath.setAutocompleter(Autocompleter.PATH_AUTOCOMPLETER);

		Rule path = sequence(PATH_NAME,
				literal("'").withName(),
				innerPath.withName("path"),
				literal("'").withName());
		path.setEvaluator(pn -> pn.evaluate("path"));
		path.setAutocompleter(new Autocompleter.EntireSequenceCompleter(this, new HashMap<>()));

		return path;
	}
}
