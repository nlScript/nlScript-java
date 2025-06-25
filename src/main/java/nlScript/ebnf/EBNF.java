package nlScript.ebnf;

import nlScript.core.Generation;
import nlScript.core.GeneratorHints;
import nlScript.core.Terminal;
import nlScript.Evaluator;
import nlScript.core.Autocompletion;
import nlScript.util.CompletePath;
import nlScript.util.RandomInt;
import nlScript.util.Range;
import nlScript.Autocompleter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Random;

public class EBNF extends EBNFCore {

	public static final String DIGIT_NAME           = Terminal.DIGIT.getSymbol();
	public static final String LETTER_NAME          = Terminal.LETTER.getSymbol();
	public static final String SIGN_NAME            = "sign";
	public static final String INTEGER_NAME         = "int";
	public static final String FLOAT_NAME           = "float";
	public static final String MONTH_NAME           = "month";
	public static final String WEEKDAY_NAME         = "weekday";
	public static final String WHITESPACE_STAR_NAME = "whitespace-star";
	public static final String WHITESPACE_PLUS_NAME = "whitespace-plus";
	public static final String INTEGER_RANGE_NAME   = "integer-range";
	public static final String PATH_NAME            = "path";
	public static final String TIME_NAME            = "time";
	public static final String DATE_NAME            = "date";
	public static final String DATETIME_NAME        = "date-time";
	public static final String COLOR_NAME           = "color";

	public final Rule SIGN;
	public final Rule INTEGER;
	public final Rule FLOAT;
	public final Rule MONTH;
	public final Rule WEEKDAY;
	public final Rule WHITESPACE_STAR;
	public final Rule WHITESPACE_PLUS;
	public final Rule INTEGER_RANGE;
	public final Rule PATH;
	public final Rule TIME;
	public final Rule DATE;
	public final Rule DATETIME;
	public final Rule COLOR;

	public EBNF() {
		SIGN            = makeSign();
		INTEGER         = makeInteger();
		FLOAT           = makeFloat();
		MONTH           = makeMonth();
		WEEKDAY         = makeWeekday();
		WHITESPACE_STAR = makeWhitespaceStar();
		WHITESPACE_PLUS = makeWhitespacePlus();
		INTEGER_RANGE   = makeIntegerRange();
		PATH            = makePath();
		TIME            = makeTime();
		DATE            = makeDate();
		DATETIME        = makeDatetime();
		COLOR           = makeColor();
		symbols.put(Terminal.DIGIT.getSymbol(), Terminal.DIGIT);
		symbols.put(Terminal.LETTER.getSymbol(), Terminal.LETTER);
	}

	public static void clearFilesystemCache() {
		CompletePath.clearFilesystemCache();
	}

	private Rule makeSign() {
		return or(SIGN_NAME, Terminal.literal("-").withName(), Terminal.literal("+").withName());
	}

	private Rule makeInteger() {
		// int -> (-|+)?digit+
		Rule ret = sequence(INTEGER_NAME,
				optional(null, SIGN.withName("sign")).withName("optional"),
				plus(null, Terminal.DIGIT.withName()).withName("plus")
		);
		ret.setEvaluator(pn -> Integer.parseInt(pn.getParsedString()));
		ret.setAutocompleter(Autocompleter.DEFAULT_INLINE_AUTOCOMPLETER);
		ret.setGenerator((grammar, hints) -> {
			int min = (int) hints.get(GeneratorHints.Key.MIN_VALUE, Integer.MIN_VALUE);
			int max = (int) hints.get(GeneratorHints.Key.MAX_VALUE, Integer.MAX_VALUE);
			return new Generation(Integer.toString(RandomInt.next(min, max)));
		});
		return ret;
	}

	private Rule makeFloat() {
		// float -> (-|+)?digit+(.digit*)?
		Rule ret = sequence(FLOAT_NAME,
				optional(null, SIGN.withName()).withName(),
				plus(null, Terminal.DIGIT.withName()).withName(),
				optional(null,
						sequence(null,
								Terminal.literal(".").withName(),
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
		ret.setGenerator((grammar, hints) -> {
			float min = (float) hints.get(GeneratorHints.Key.MIN_VALUE, Float.MIN_VALUE);
			float max = (float) hints.get(GeneratorHints.Key.MAX_VALUE, Float.MAX_VALUE);
			float f = min + (max - min) * (float) Math.random();
			int decimalPlaces = (int) hints.get(GeneratorHints.Key.DECIMAL_PLACES, -1);
			String fStr = decimalPlaces == -1 ? Float.toString(f) : format(f, decimalPlaces);
			return new Generation(fStr);
		});
		return ret;
	}

	private String format(float f, int decimalDigits) {
		StringBuilder sb = new StringBuilder("#");
		if(decimalDigits > 0)
			sb.append('.');
		for(int i = 0; i < decimalDigits; i++)
			sb.append('#');

		DecimalFormat df = new DecimalFormat(sb.toString());
		DecimalFormatSymbols dfs = new DecimalFormatSymbols();
		dfs.setDecimalSeparator('.');
		df.setDecimalFormatSymbols(dfs);
		df.setGroupingUsed(false);
		return df.format(f);
	}

	private Rule makeWhitespaceStar() {
		Rule ret = star(WHITESPACE_STAR_NAME, Terminal.WHITESPACE.withName());
		ret.setAutocompleter((pn, justCheck) -> Autocompletion.literal(pn, pn.getParsedString().isEmpty() ? " " : ""));
		ret.setGenerator((grammar, hints) -> new Generation(" "));
		return ret;
	}

	private Rule makeWhitespacePlus() {
		Rule ret = plus(WHITESPACE_PLUS_NAME, Terminal.WHITESPACE.withName());
		ret.setAutocompleter((pn, justCheck) -> Autocompletion.literal(pn, pn.getParsedString().isEmpty() ? " " : ""));
		ret.setGenerator((grammar, hints) -> new Generation(" "));
		return ret;
	}

	private Rule makeIntegerRange() {
		Rule delimiter = sequence(null,
				WHITESPACE_STAR.withName("ws*"),
				Terminal.literal("-").withName(),
				WHITESPACE_STAR.withName("ws*"));
		Rule ret = join(INTEGER_RANGE_NAME,
				INTEGER.withName(),
				null,
				null,
				delimiter.getTarget().withName("delimiter"),
				"from", "to");
		ret.setEvaluator(pn -> new Range(
				(Integer) pn.evaluate(0),
				(Integer) pn.evaluate(1)));
		return ret;
		// TODO set autocompleter
	}

	private Rule makeColor() {
		Rule black       = sequence(null, Terminal.literal("black"       ).withName()).setEvaluator(pn -> rgb2int(  0,   0,   0));
		Rule white       = sequence(null, Terminal.literal("white"       ).withName()).setEvaluator(pn -> rgb2int(255, 255, 255));
		Rule red         = sequence(null, Terminal.literal("red"         ).withName()).setEvaluator(pn -> rgb2int(255,   0,   0));
		Rule orange      = sequence(null, Terminal.literal("orange"      ).withName()).setEvaluator(pn -> rgb2int(255, 128,   0));
		Rule yellow      = sequence(null, Terminal.literal("yellow"      ).withName()).setEvaluator(pn -> rgb2int(255, 255,   0));
		Rule lawngreen   = sequence(null, Terminal.literal("lawn green"  ).withName()).setEvaluator(pn -> rgb2int(128, 255,   0));
		Rule green       = sequence(null, Terminal.literal("green"       ).withName()).setEvaluator(pn -> rgb2int(  0, 255,   0));
		Rule springgreen = sequence(null, Terminal.literal("spring green").withName()).setEvaluator(pn -> rgb2int(  0, 255, 180));
		Rule cyan        = sequence(null, Terminal.literal("cyan"        ).withName()).setEvaluator(pn -> rgb2int(  0, 255, 255));
		Rule azure       = sequence(null, Terminal.literal("azure"       ).withName()).setEvaluator(pn -> rgb2int(  0, 128, 255));
		Rule blue        = sequence(null, Terminal.literal("blue"        ).withName()).setEvaluator(pn -> rgb2int(  0,   0, 255));
		Rule violet      = sequence(null, Terminal.literal("violet"      ).withName()).setEvaluator(pn -> rgb2int(128,   0, 255));
		Rule magenta     = sequence(null, Terminal.literal("magenta"     ).withName()).setEvaluator(pn -> rgb2int(255,   0, 255));
		Rule pink        = sequence(null, Terminal.literal("pink"        ).withName()).setEvaluator(pn -> rgb2int(255,   0, 128));
		Rule gray        = sequence(null, Terminal.literal("gray"        ).withName()).setEvaluator(pn -> rgb2int(128, 128, 128));

		Rule custom = tuple(null, INTEGER.withName(), "red", "green", "blue");
		custom.setEvaluator(pn -> {
			int r = (Integer) pn.evaluate("red");
			int g = (Integer) pn.evaluate("green");
			int b = (Integer) pn.evaluate("blue");
			return rgb2int(r, g, b);
		});
		custom.setGenerator((grammar, hints) -> {
			int r = RandomInt.next(0, 255);
			int g = RandomInt.next(0, 255);
			int b = RandomInt.next(0, 255);
			return new Generation("(" + r + ", " + g + ", " + b + ")");
		});

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
		Rule hour = sequence(null,
				optional(null, Terminal.DIGIT.withName()).withName(),
				Terminal.DIGIT.withName());
		hour.setAutocompleter(Autocompleter.DEFAULT_INLINE_AUTOCOMPLETER);

		Rule minute = sequence(null,
				Terminal.DIGIT.withName(),
				Terminal.DIGIT.withName());
		minute.setAutocompleter(Autocompleter.DEFAULT_INLINE_AUTOCOMPLETER);

		Rule ret = sequence(TIME_NAME,
				hour.withName("HH"),
				Terminal.literal(":").withName(),
				minute.withName("MM"));

		ret.setGenerator((grammar, hints) -> {
			Random random = new Random();
			int h = random.nextInt(24);
			int m = random.nextInt(60);
			String mm = Integer.toString(m);
			if(m < 10) mm = "0" + mm;
			return new Generation(h + ":" + mm);
		});

		ret.setEvaluator(pn -> LocalTime.parse(pn.getParsedString(), DateTimeFormatter.ofPattern("H:mm")));
		ret.setAutocompleter(new Autocompleter.EntireSequenceCompleter(this, new HashMap<>()));

		return ret;
	}

	private Rule makeMonth() {
		return or(MONTH_NAME,
			sequence(null, Terminal.literal("January")  .withName()).setEvaluator(pn -> 0) .withName("january"),
			sequence(null, Terminal.literal("February") .withName()).setEvaluator(pn -> 1) .withName("february"),
			sequence(null, Terminal.literal("March")    .withName()).setEvaluator(pn -> 2) .withName("march"),
			sequence(null, Terminal.literal("April")    .withName()).setEvaluator(pn -> 3) .withName("april"),
			sequence(null, Terminal.literal("May")      .withName()).setEvaluator(pn -> 4) .withName("may"),
			sequence(null, Terminal.literal("June")     .withName()).setEvaluator(pn -> 5) .withName("june"),
			sequence(null, Terminal.literal("July")     .withName()).setEvaluator(pn -> 6) .withName("july"),
			sequence(null, Terminal.literal("August")   .withName()).setEvaluator(pn -> 7) .withName("august"),
			sequence(null, Terminal.literal("September").withName()).setEvaluator(pn -> 8) .withName("september"),
			sequence(null, Terminal.literal("October")  .withName()).setEvaluator(pn -> 9) .withName("october"),
			sequence(null, Terminal.literal("November") .withName()).setEvaluator(pn -> 10).withName("november"),
			sequence(null, Terminal.literal("December") .withName()).setEvaluator(pn -> 11).withName("december"));
	}

	private Rule makeWeekday() {
		return or(WEEKDAY_NAME,
				sequence(null, Terminal.literal("Monday")   .withName()).setEvaluator(pn -> 0) .withName("monday"),
				sequence(null, Terminal.literal("Tuesday")  .withName()).setEvaluator(pn -> 1) .withName("tuesday"),
				sequence(null, Terminal.literal("Wednesday").withName()).setEvaluator(pn -> 2) .withName("wednesday"),
				sequence(null, Terminal.literal("Thursday") .withName()).setEvaluator(pn -> 3) .withName("thursday"),
				sequence(null, Terminal.literal("Friday")   .withName()).setEvaluator(pn -> 4) .withName("friday"),
				sequence(null, Terminal.literal("Saturday") .withName()).setEvaluator(pn -> 5) .withName("saturday"),
				sequence(null, Terminal.literal("Sunday")   .withName()).setEvaluator(pn -> 6) .withName("sunday"));
	}

	private Rule makeDate() {
		Rule day = sequence(null,
				optional(null, Terminal.DIGIT.withName()).withName(),
				Terminal.DIGIT.withName());
		day.setAutocompleter(Autocompleter.DEFAULT_INLINE_AUTOCOMPLETER);
		Rule year = sequence(null,
				Terminal.DIGIT.withName(),
				Terminal.DIGIT.withName(),
				Terminal.DIGIT.withName(),
				Terminal.DIGIT.withName());
		year.setAutocompleter(Autocompleter.DEFAULT_INLINE_AUTOCOMPLETER);
		Rule ret = sequence(DATE_NAME,
				day.withName("day"),
				Terminal.literal(" ").withName(),
				MONTH.withName("month"),
				Terminal.literal(" ").withName(),
				year.withName("year"));
		ret.setEvaluator(pn -> LocalDate.parse(pn.getParsedString(), DateTimeFormatter.ofPattern("d MMMM yyyy")));
		ret.setAutocompleter(new Autocompleter.EntireSequenceCompleter(this, new HashMap<>()));

		return ret;
	}

	private Rule makeDatetime() {
		Rule ret = sequence(DATETIME_NAME,
				DATE.withName("date"),
				Terminal.literal(" ").withName(),
				TIME.withName("time"));
		ret.setEvaluator(pn -> {
			LocalDate date = (LocalDate) pn.evaluate("date");
			LocalTime time = (LocalTime) pn.evaluate("time");
			return LocalDateTime.of(date, time);
		});
		ret.setAutocompleter(new Autocompleter.EntireSequenceCompleter(this, new HashMap<>()));
		return ret;
	}

	private Rule makePath() {
		Rule innerPath = plus(null,
				Terminal.characterClass("[^'<>|?*\n]").withName("inner-path"));
		innerPath.setEvaluator(Evaluator.DEFAULT_EVALUATOR);
		innerPath.setAutocompleter(Autocompleter.PATH_AUTOCOMPLETER);

		Rule path = sequence(PATH_NAME,
				Terminal.literal("'").withName(),
				innerPath.withName("path"),
				Terminal.literal("'").withName());
		path.setEvaluator(pn -> pn.evaluate("path"));
		path.setAutocompleter(new Autocompleter.EntireSequenceCompleter(this, new HashMap<>()));

		return path;
	}
}
