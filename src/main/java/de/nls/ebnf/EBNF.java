package de.nls.ebnf;

import de.nls.Autocompleter;
import de.nls.Evaluator;
import de.nls.ParsedNode;
import de.nls.core.DefaultParsedNode;
import de.nls.util.Range;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static de.nls.ebnf.Named.n;
import static de.nls.core.Terminal.*;

public class EBNF extends EBNFCore {

	public static final String SIGN_NAME            = "sign";
	public static final String INTEGER_NAME         = "int";
	public static final String FLOAT_NAME           = "float";
	public static final String WHITESPACE_STAR_NAME = "whitespace-star";
	public static final String WHITESPACE_PLUS_NAME = "whitespace-plus";
	public static final String INTEGER_RANGE_NAME   = "integer-range";
	public static final String PATH_NAME            = "path";
	public static final String TIME_NAME            = "time";
	public static final String COLOR_NAME           = "color";

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
		pathAutocompleter.clearFilesystemCache();
	}

	private Rule makeSign() {
		return or(SIGN_NAME, literal("-").withName(), literal("+").withName());
	}

	private Rule makeInteger() {
		// int -> (-|+)?digit+
		Rule ret = sequence(INTEGER_NAME,
				optional(null, SIGN.withName("sign")).withName("optional"),
				plus(null, DIGIT.withName()).withName("plus")
		);
		ret.setEvaluator(pn -> Integer.parseInt(pn.getParsedString()));
		ret.setAutocompleter(Autocompleter.DEFAULT_INLINE_AUTOCOMPLETER);
		return ret;
	}

	private Rule makeFloat() {
		// float -> (-|+)?digit+(.digit*)?
		Rule ret = sequence(FLOAT_NAME,
				optional(null, SIGN.withName()).withName(),
				plus(null, DIGIT.withName()).withName(),
				optional(null,
						sequence(null,
								literal(".").withName(),
								star(null, DIGIT.withName()).withName("star")
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
		Rule ret = star(WHITESPACE_STAR_NAME, WHITESPACE.withName());
		ret.setAutocompleter(new Autocompleter.IfNothingYetEnteredAutocompleter(" "));
		return ret;
	}

	private Rule makeWhitespacePlus() {
		Rule ret = plus(WHITESPACE_PLUS_NAME, WHITESPACE.withName());
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
				optional(null, DIGIT.withName()).withName(),
				DIGIT.withName(),
				literal(":").withName(),
				DIGIT.withName(),
				DIGIT.withName());
		ret.setEvaluator(pn -> LocalTime.parse(pn.getParsedString(), DateTimeFormatter.ofPattern("H:mm")));
		ret.setAutocompleter(new Autocompleter.IfNothingYetEnteredAutocompleter("${HH}:${MM}"));
		return ret;
	}

	private Rule makePath() {
		Rule innerPath = plus(null,
		innerPath.setAutocompleter(pathAutocompleter);
				characterClass("[^'<>|?*\n]").withName("inner-path"));
		innerPath.setEvaluator(Evaluator.DEFAULT_EVALUATOR);

		Rule path = sequence(PATH_NAME,
				literal("'").withName(),
				innerPath.withName("path"),
				literal("'").withName());
		path.setEvaluator(pn -> pn.evaluate("path"));
		path.setAutocompleter(new Autocompleter.EntireSequenceCompleter(this, new HashMap<>()));

		return path;
	}

	private static final PathAutocompleter pathAutocompleter = new PathAutocompleter();

	private static final class PathAutocompleter implements Autocompleter {

		static HashMap<Path, Path[]> filesystemCache = new HashMap<>();

		static void clearFilesystemCache() {
			filesystemCache.clear();
		}

		/**
		 * Returns the parent node of the entered path;
		 * - if the entered path doesn't contain a root element (i.e. is not absolute), it returns null
		 * - if the entered path is a directory but does not end with a separator, it returns the path's parent itself
		 * - if the entered path is a directory and does end with a separator, it returns the path itself
		 */
		static Path getParent(String alreadyEntered) {
			Path p = Paths.get(alreadyEntered);
			if(!p.isAbsolute())
				return null;
			if(alreadyEntered.endsWith(FileSystems.getDefault().getSeparator()) ||
					alreadyEntered.endsWith("/"))
				return p;
			return p.getParent();
		}

		/**
		 * Returns the file part of the entered path, relative to <code>getParent()</code>
		 */
		static Path getChild(String alreadyEntered) {
			Path child = Paths.get(alreadyEntered);
			Path parent = getParent(alreadyEntered);
			if(parent == null)
				return child;
			return parent.relativize(child);
		}

		/**
		 * Returns the file name as a String; unlike Path.getFileName(), it also treats the filesystem root as a file.
		 */
		static String getFileName(Path path) {
			Path name = path.getFileName();
			if(name != null)
				return name.toString();
			return path.toString();
		}

		/**
		 * Unlike Files.isHidden, also works for directories on Windows.
		 */
		static boolean isHidden(Path path) {
			try {
				return getFileName(path).startsWith(".") || Files.isHidden(path) || (Boolean) Files.getAttribute(path, "dos:hidden");
			} catch(Exception e) {
				return false;
			}
		}

		static String[] getSiblings(String alreadyEntered) {
			Path parent = getParent(alreadyEntered);
			Path child = getChild(alreadyEntered);

			Path[] siblingsArray = filesystemCache.get(parent);

			if(siblingsArray == null) {
				try {
					Stream<Path> siblings = parent == null
							? StreamSupport.stream(FileSystems.getDefault().getRootDirectories().spliterator(), true)
							: Files.list(parent);
					siblingsArray = siblings.toArray(Path[]::new);
					filesystemCache.put(parent, siblingsArray);
				} catch (Exception e) {
					return new String[0];
				}
			}

			return Arrays.stream(siblingsArray)
					.filter(p -> getFileName(p).toLowerCase().startsWith(child.toString().toLowerCase()))
					.map(PathWrapper::new)
					.sorted((o1, o2) -> {
						if(o1.isHidden && !o2.isHidden) return +1; // o2 > o1
						if(o2.isHidden && !o1.isHidden) return -1; // o1 < o2
						if(o1.isDirectory && !o2.isDirectory) return -1;
						if(o2.isDirectory && !o1.isDirectory) return +1;
						return o1.name.compareTo(o2.name);
					})
					.map(p -> {
						String s = p.path.toString();
						if(p.isDirectory)
							s += FileSystems.getDefault().getSeparator();
						return s;
					})
					.toArray(String[]::new);
		}

		static String getCompletion(String alreadyEntered) {
			String[] siblings = getSiblings(alreadyEntered);
			if(siblings.length == 0)
				return alreadyEntered;
			return String.join(";;;", siblings);
		}

		public String getAutocompletion(ParsedNode p) {
			String ret = getCompletion(p.getParsedString());
			System.out.println("getAutocompletion: " + ret);
			return ret;
		}

		private static class PathWrapper {
			private final boolean isHidden;
			private final boolean isDirectory;
			private final Path path;
			private final String name;

			private PathWrapper(Path path) {
				this.path = path;
				this.name = getFileName(path);
				this.isHidden = isHidden(path);
				this.isDirectory = Files.isDirectory(path);
			}
		}
	}

	private static void testFileName() {
		Path p = Paths.get("c:\\");
		System.out.println(PathAutocompleter.getFileName(p));

		p = Paths.get("c:");
		System.out.println(PathAutocompleter.getFileName(p));

		p = Paths.get("c");
		System.out.println(PathAutocompleter.getFileName(p));

		p = Paths.get("c:\\users");
		System.out.println(PathAutocompleter.getFileName(p));

		p = Paths.get("c:\\users\\");
		System.out.println(PathAutocompleter.getFileName(p));

		p = Paths.get("c:/users");
		System.out.println(PathAutocompleter.getFileName(p));

		p = Paths.get("c:/users/");
		System.out.println(PathAutocompleter.getFileName(p));
	}

	public static void main(String[] args) {
		testFileName();
	}
}
