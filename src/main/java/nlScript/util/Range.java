package nlScript.util;

public class Range {
	final int lower;
	final int upper;

	public static final Range STAR     = new Range(0, Integer.MAX_VALUE);
	public static final Range PLUS     = new Range(1, Integer.MAX_VALUE);
	public static final Range OPTIONAL = new Range(0, 1);

	public Range(int lowerUpper) {
		this(lowerUpper, lowerUpper);
	}

	public Range(int lower, int upper) {
		this.lower = lower;
		this.upper = upper;
	}

	public int getLower() {
		return lower;
	}

	public int getUpper() {
		return upper;
	}

	public boolean equals(Object o) {
		if(! (o instanceof Range))
			return false;
		Range r = (Range)o;
		return this.lower == r.lower && this.upper == r.upper;
	}

	public int hashCode() {
		return 31 * lower + upper;
	}

	public String toString() {
		return "[" + lower + " - " + upper + "]";
	}
}
