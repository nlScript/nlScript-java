package de.nls.core;

public class Lexer {

	private final String input;

	private int pos = 0;

	public Lexer(String input) {
		this.input = input;
	}

	public int getPosition() {
		return pos;
	}

	public void fwd(int len) {
		pos += len;
	}

	public char peek() {
		return pos < input.length() ? peek(0) : '$';
	}

	public void setPosition(int pos) {
		this.pos = pos;
	}

	public char peek(int n) {
		return input.charAt(pos + n);
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
}
