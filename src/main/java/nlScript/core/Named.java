package nlScript.core;

public class Named<T extends RepresentsSymbol> {

	public static final String UNNAMED = "UNNAMED";

	public final String name;

	public final T object;

	public Named(T object, String name) {
		this.object = object;
		this.name = name;
	}

	public Named(T object) {
		this.object = object;
		this.name = UNNAMED;
	}

	public String getName() {
		return name != null ? name : UNNAMED;
	}

	public T get() {
		return object;
	}

	public Symbol getSymbol() {
		return object.getRepresentedSymbol();
	}
}
