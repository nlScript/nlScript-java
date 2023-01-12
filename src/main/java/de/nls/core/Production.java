package de.nls.core;

import de.nls.ParsedNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Production {

	private final NonTerminal left;
	private final Symbol[] right;

	private AstBuilder astBuilder = null;
	private ExtensionListener extensionListener = null;

	public interface AstBuilder {
		void buildAST(ParsedNode parent, ParsedNode... children);
	}

	public interface ExtensionListener {
		void onExtension(ParsedNode parent, ParsedNode... children);
	}

	public Production(NonTerminal left, Symbol... right) {
		this.left = left;
		this.right = removeEpsilon(right);
	}

	private static Symbol[] removeEpsilon(Symbol[] arr) {
		List<Symbol> list = new ArrayList<>(Arrays.asList(arr));
		int idx = list.indexOf(Terminal.EPSILON);
		if(idx == -1)
			return arr;
		list.remove(Terminal.EPSILON);
		return list.toArray(new Symbol[0]);
	}

	public NonTerminal getLeft() {
		return left;
	}

	public Symbol[] getRight() {
		return right;
	}

	public void setAstBuilder(AstBuilder astBuilder) {
		this.astBuilder = astBuilder;
	}

	public void builtAST(ParsedNode parent, ParsedNode... children) {
		if(astBuilder != null) {
			astBuilder.buildAST(parent, children);
			return;
		}
		parent.addChildren(children);
	}

	public void wasExtended(ParsedNode parent, ParsedNode... children) {
		if(this.extensionListener != null)
			extensionListener.onExtension(parent, children);
	}

	public void onExtension(ExtensionListener listener) {
		if(this.extensionListener != null)
			throw new RuntimeException("ExtensionListener cannot be overwritten");
		this.extensionListener = listener;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		String left = getLeft().toString();
		for(int k = 0; k < (50 - left.length()); k++)
			sb.append(' ');
		sb.append(left);
		sb.append(" -> ");
		Symbol[] right = getRight();
		for(Symbol symbol : right) {
			sb.append(symbol).append(" ");
		}
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if(o.getClass() != getClass())
			return false;
		Production p = (Production) o;
		return left.equals(p.left) && Arrays.equals(right, p.right);
	}

	@Override
	public int hashCode() {
		return Objects.hash(left, Arrays.hashCode(right));
	}
}
