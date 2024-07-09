package nlScript.core;

public class Lexer {

	private final String input;

	private int pos = 0;

	public Lexer(String input) {
		this.input = input;
	}

	public int getPosition() {
		return pos;
	}

	public void setPosition(int pos) {
		this.pos = pos;
	}

	public void fwd(int len) {
		pos += len;
	}

	public char peek() {
		return peek(0);
	}

	public char peek(int n) {
		int p = pos + n;
		return p < input.length() ? input.charAt(p) : '$';
	}

	public String substring(int from, int to) {
		if(to > input.length())
			to = input.length();
		return input.substring(from, to);
	}

	public String substring(int from) {
		return input.substring(from);
	}

	public boolean isDone() {
		return pos > input.length();
	}

	public boolean isAtEnd() {
		return isAtEnd(0);
	}

	public boolean isAtEnd(int fwd) {
		return pos + fwd == input.length();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(input.substring(0, pos)).append(" -- ").append(input.substring(pos));
		return sb.toString();
	}
}
