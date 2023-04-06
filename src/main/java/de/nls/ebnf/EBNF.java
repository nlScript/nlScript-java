package de.nls.ebnf;

import de.nls.Autocompleter;
import de.nls.ParsedNode;
import de.nls.core.DefaultParsedNode;
import de.nls.util.Range;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

	public final Rule SIGN;
	public final Rule INTEGER;
	public final Rule FLOAT;
	public final Rule WHITESPACE_STAR;
	public final Rule WHITESPACE_PLUS;
	public final Rule INTEGER_RANGE;
	public final Rule PATH;

	public EBNF() {
		SIGN            = makeSign();
		INTEGER         = makeInteger();
		FLOAT           = makeFloat();
		WHITESPACE_STAR = makeWhitespaceStar();
		WHITESPACE_PLUS = makeWhitespacePlus();
		INTEGER_RANGE   = makeIntegerRange();
		PATH            = makePath();
	}

	public static void clearFilesystemCache() {
		pathAutocompleter.clearFilesystemCache();
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
		Rule ret = plus(WHITESPACE_PLUS_NAME, n(WHITESPACE));
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

	private Rule makePath() {
		Rule innerPath = plus(null,
				n("inner-path", characterClass("[^'<>|?*\n]")));
		innerPath.setEvaluator(DefaultParsedNode::getParsedString);
		innerPath.setAutocompleter(pathAutocompleter);

		Rule path = sequence(PATH_NAME,
				n(literal("'")),
				n("path", innerPath),
				n(literal("'")));
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
						if(o1.isHidden && !o2.isHidden) return +1;
						if(o2.isHidden && !o1.isHidden) return -1;
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
}
