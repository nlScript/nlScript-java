package de.nlScript.core;

public enum ParsingState {
	SUCCESSFUL,
	END_OF_INPUT,
	FAILED,
	NOT_PARSED;

	public boolean isBetterThan(ParsingState o) {
		if(this.ordinal() < o.ordinal())
			return true;
		if(this.ordinal() > o.ordinal())
			return false;
		return false;
	}
}
