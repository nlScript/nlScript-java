package nlScript.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Generation {

	private final ArrayList<Generation> children = new ArrayList<>();
	private final String generatedText;

	private String name;

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

	public String getGeneratedText(String... names) {
		Generation pn = this;
		for(String name : names) {
			pn = pn.getChild(name);
			if(pn == null)
				return "";
		}
		return pn.generatedText;
	}
}
