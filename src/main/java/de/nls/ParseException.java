package de.nls;

import de.nls.core.BNF;
import de.nls.core.DefaultParsedNode;
import de.nls.core.Lexer;
import de.nls.core.Autocompletion;
import de.nls.core.RDParser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ParseException extends Exception {

	private final DefaultParsedNode root;

	private final DefaultParsedNode failedTerminal;

	private final RDParser parser;

	private final DefaultParsedNode firstAutocompletingAncestorThatFailed;

	public ParseException(DefaultParsedNode root, DefaultParsedNode failedTerminal, RDParser parser) {
		super();
		this.root = root;
		this.failedTerminal = failedTerminal;
		this.parser = parser;

		DefaultParsedNode tmp = failedTerminal;
		while(tmp != null && !tmp.doesAutocomplete())
			tmp = tmp.getParent();
		firstAutocompletingAncestorThatFailed = tmp;
	}

	public String getMessage() {
		return getError();
	}

	public DefaultParsedNode getRoot() {
		return root;
	}

	public DefaultParsedNode getFailedTerminal() {
		return failedTerminal;
	}

	public DefaultParsedNode getFirstAutocompletingAncestorThatFailed() {
		return firstAutocompletingAncestorThatFailed;
	}

	public String getError() {
		final Lexer lexer = parser.getLexer();
		final BNF grammar = parser.getGrammar();

		int errorPos = failedTerminal.getMatcher().pos + failedTerminal.getMatcher().parsed.length() - 1;

		// the character at last.matcher.pos failed, everything before must have been working
		String workingText = lexer.substring(0, failedTerminal.getMatcher().pos);
		// create a new parser and collect the autocompletions
		Lexer workingLexer = new Lexer(workingText);
		RDParser parser2 = new RDParser(grammar, workingLexer, parser.getParsedNodeFactory());
		ArrayList<Autocompletion> expectations = new ArrayList<>();
		try {
			parser2.parse(expectations);
		} catch (ParseException e) {
			return "Error at position " + errorPos;
		}

		String[] lines = lexer.substring(0, errorPos + 1).split("\\r?\\n|\\r");
		int errorLine = lines.length - 1;
		int errorPosInLastLine = lines[errorLine].length() - 1;

		StringBuilder errorMessage = new StringBuilder();
		final String nl = System.lineSeparator();
		errorMessage.append("Error at position ").append(errorPos).append(" in line ").append(errorLine).append(":").append(nl);
		errorMessage.append(lines[errorLine]).append(nl);
		for(int i = 0; i < errorPosInLastLine; i++)
			errorMessage.append(" ");
		errorMessage.append("^").append(nl);

		List<String> exString = expectations.stream()
				.map(ac -> ac.getCompletion(Autocompletion.Purpose.FOR_INSERTION))
				.collect(Collectors.toList());
		errorMessage.append("Expected ").append(exString);

		return errorMessage.toString();
	}
}
