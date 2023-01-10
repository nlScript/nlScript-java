package de.nls.core;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class GraphViz {

	public static String toVizDotLink(ParsedNode root) {
		try {
			return "https://edotor.net/?s=%22bla%22?engine=dot#" +
					URLEncoder.encode(toVizDot(root), StandardCharsets.UTF_8.toString())
							.replace("+", "%20")
							.replace("*", "%2A");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public static String toVizDot(ParsedNode root) {
		return "digraph parsed_tree {\n" +
				"  # rankdir=LR;\n" +
				"  size=\"8,5\"\n" +
				"  node [shape=circle];\n\n" +
				vizDotNodes(root) +
				"\n" +
				vizDotLinks(root) +
				"}\n";
	}

	private static String vizDotNodes(ParsedNode root) {
		String color = "black";
		Matcher matcher = root.getMatcher();
		if(matcher != null) {
			switch (matcher.state) {
				case SUCCESSFUL:
					color = "green";
					break;
				case END_OF_INPUT:
					color = "orange";
					break;
				case FAILED:
					color = "red3";
					break;
			}
		}
		StringBuilder sb = new StringBuilder();
		sb.append("  ")
				.append(root.hashCode())
				.append("[label=\"").append(root.getSymbol()).append("\", color=").append(color)
				.append(", tooltip=\"").append(root.getParsedString()).append("\"")
				.append("]\n");
		for(ParsedNode pn : root.getChildren())
			sb.append(vizDotNodes(pn));
		return sb.toString();
	}

	private static String vizDotLinks(ParsedNode root) {
		StringBuilder sb = new StringBuilder();
		int hash = root.hashCode();
		for (ParsedNode child : root.getChildren()) {
			sb.append("  ").append(hash).append(" -> ").append(child.hashCode()).append(";\n");
			sb.append(vizDotLinks(child));
		}
		return sb.toString();
	}
}
