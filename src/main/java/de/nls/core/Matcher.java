package de.nls.core;

public class Matcher {

	public final ParsingState state;
	public final int pos;
	public final String parsed;

	public Matcher(ParsingState state, int pos, String parsed) {
		this.state = state;
		this.pos = pos;
		this.parsed = parsed;
	}

	public boolean isBetterThan(Matcher o) {
		if(o == null)
			return true;
		if(state.isBetterThan(o.state))
			return true;
		if(o.state.isBetterThan(this.state))
			return false;
		int tParsedLength = pos + parsed.length();
		int oParsedLength = o.pos + o.parsed.length();
		if(tParsedLength > oParsedLength)
			return true;
		if(tParsedLength < oParsedLength)
			return false;
		return false;
	}

	public String toString() {
		return state.toString() + ": '" + parsed + "' (" + pos + ")";
	}
}
