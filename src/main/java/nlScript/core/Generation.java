package nlScript.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Generation {

	private final ArrayList<Generation> children = new ArrayList<>();
	private final String generatedText;

	private String name;
	private String description;

	public Generation(String text, Generation... children) {
		this.generatedText = text;
		this.children.addAll(Arrays.asList(children));
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}


	public String getDescription() {
		return description;
	}

	public Generation setDescription(String description) {
		this.description = description;
		return this;
	}

	public List<Generation> getChildren() {
		return children;
	}

	public Generation getChild(int i) {
		return children.get(i);
	}

	public Generation getChild(String name) {
		for(Generation n : children)
			if (name.equals(n.getName()))
				return n;
		return null;
	}

	public String toString() {
		return generatedText;
	}

	public Generation getChild(String... names) {
		Generation pn = this;
		for(String name : names) {
			pn = pn.getChild(name);
			if(pn == null)
				return null;
		}
		return pn;
	}

	public String getGeneratedText(String... names) {
		Generation pn = getChild(names);
		return pn == null ? "" : pn.generatedText;
	}

	/**
	 * Returns a new Generation, concatenating this Generation's generated text with the given text.
	 * Replaces any occurrence of '{childName}' with the generated text of the respective child. Grandchildren can
	 * be accessed using '{childName::grandChildName}'.
	 */
	public Generation withAppendedText(String textToAppend) {
		return withAppendedText(textToAppend, false);
	}

	/**
	 * Returns a new Generation, concatenating this Generation's generated text with the given text.
	 * If <code>skipVariableSubstitution</code> is false, replaces any occurrence of '{childName}' with the
	 * generated text of the respective child. Grandchildren can be accessed using '{childName::grandChildName}'.
	 */
	public Generation withAppendedText(String textToAppend, boolean skipVariableSubstitution) {
		if(!skipVariableSubstitution)
			textToAppend = processText(textToAppend);
		return new Generation(generatedText + textToAppend, this.children.toArray(new Generation[0]));
	}

	/**
	 * Returns a new Generation, prepending this Generation's generated text with the given text.
	 * Replaces any occurrence of '{childName}' with the generated text of the respective child. Grandchildren can
	 * be accessed using '{childName::grandChildName}'.
	 */
	public Generation withPrependedText(String textToPrepend) {
		return withPrependedText(textToPrepend, false);
	}

	/**
	 * Returns a new Generation, prepending this Generation's generated text with the given text.
	 * If <code>skipVariableSubstitution</code> is false, replaces any occurrence of '{childName}' with the
	 * generated text of the respective child. Grandchildren can be accessed using '{childName::grandChildName}'.
	 */
	public Generation withPrependedText(String textToPrepend, boolean skipVariableSubstitution) {
		if(!skipVariableSubstitution)
			textToPrepend = processText(textToPrepend);
		return new Generation(textToPrepend + generatedText, this.children.toArray(new Generation[0]));
	}

	/**
	 * Returns a new Generation, replacing this Generation's generated text with the given text.
	 * Replaces any occurrence of '{childName}' with the generated text of the respective child. Grandchildren can
	 * be accessed using '{childName::grandChildName}'.
	 */
	public Generation withText(String textReplacement) {
		return withText(textReplacement, false);
	}

	/**
	 * Returns a new Generation, replacing this Generation's generated text with the given text.
	 * If <code>skipVariableSubstitution</code> is false, replaces any occurrence of '{childName}' with the
	 * generated text of the respective child. Grandchildren can be accessed using '{childName::grandChildName}'.
	 */
	public Generation withText(String textReplacement, boolean skipVariableSubstitution) {
		if(!skipVariableSubstitution)
			textReplacement = processText(textReplacement);
		return new Generation(textReplacement, this.children.toArray(new Generation[0]));
	}

	public String processText(String text) {
		return VariableProcessor.replace(text, s -> {
			boolean join = s.startsWith("join [");
			String delimiter = "";
			if(join) {
				int bracketOpen = s.indexOf('[');
				int bracketClose = s.indexOf(']');
				delimiter = s.substring(bracketOpen + 1, bracketClose);
				int substrStart = bracketClose + 1;
				while(s.charAt(substrStart) == ' ')
					substrStart++;
				s = s.substring(substrStart);
			}


			boolean desc = s.endsWith(".description");
			if(desc)
				s = s.substring(0, s.length() - ".description".length());

			String[] childNames = s.split("::");
			Generation gen = getChild(childNames);
			if(gen == null)
				throw new RuntimeException("Can't find child " + Arrays.toString(childNames));

			if(!join) {
				if(!desc)
					return gen.generatedText;
				String description = gen.getDescription();
				if(description == null)
					throw new RuntimeException("No description given for '" + Arrays.toString(childNames) + "'");
				return description;
			}

			return gen.children.stream()
					.map(c -> {
						if(desc && c.getDescription() == null)
							throw new RuntimeException("No description given for '" + Arrays.toString(childNames) + ", " + c.getName() + "'");
						return desc ? c.getDescription() : c.generatedText;
					})
					.collect(Collectors.joining(delimiter));
		});
	}

	private static class VariableProcessor {
		// matches '{anything-but-closing-braces}' and groups the inner part in group 1
		private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)}");

		public static String replace(String input, Function<String, String> getReplacement) {
			java.util.regex.Matcher matcher = PLACEHOLDER_PATTERN.matcher(input);
			StringBuffer result = new StringBuffer();

			while (matcher.find()) {
				String matchedString = matcher.group(1);
				String replacement = getReplacement.apply(matchedString);
				if(replacement == null)
					throw new RuntimeException("Can't find replacement for " + matchedString + " in " + input);
				matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
			}

			matcher.appendTail(result);
			return result.toString();
		}
	}
}
