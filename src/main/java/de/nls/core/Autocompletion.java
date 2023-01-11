package de.nls.core;

public class Autocompletion {
	private final String completion;
	private final String alreadyEnteredText;

	public Autocompletion(String completion, String alreadyEnteredText) {
		this.completion = completion;
		this.alreadyEnteredText = alreadyEnteredText;
	}

	public String getCompletion() {
		return completion;
	}

	public String getAlreadyEnteredText() {
		return alreadyEnteredText;
	}

	public boolean equals(Object o) {
		if(o instanceof  Autocompletion)
			return ((Autocompletion) o).completion.equals(completion);
		return false;
	}

	public int hashCode() {
		return completion.hashCode();
	}

	public String toString() {
		return completion;
	}
}
