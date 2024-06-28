package de.nlScript.util;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CompletePath {

	static HashMap<Path, Path[]> filesystemCache = new HashMap<>();

	public static void clearFilesystemCache() {
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

	public static String[] getCompletion(String alreadyEntered) {
		String[] siblings = getSiblings(alreadyEntered);
		if(siblings.length == 0)
			return new String[] { alreadyEntered };
		return siblings;
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

	private static void testFileName() {
		Path p = Paths.get("c:\\");
		System.out.println(getFileName(p));

		p = Paths.get("c:");
		System.out.println(getFileName(p));

		p = Paths.get("c");
		System.out.println(getFileName(p));

		p = Paths.get("c:\\users");
		System.out.println(getFileName(p));

		p = Paths.get("c:\\users\\");
		System.out.println(getFileName(p));

		p = Paths.get("c:/users");
		System.out.println(getFileName(p));

		p = Paths.get("c:/users/");
		System.out.println(getFileName(p));
	}

	public static void main(String[] args) {
		testFileName();
	}
}