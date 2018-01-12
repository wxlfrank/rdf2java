package org.open.generate;

public class Interface extends Type {

	public Interface(Package pack, String name) {
		setName(name);
		setContainer(pack);
		if (pack != null)
			pack.addInterface(this);
	}

	public Interface() {
	}

	public void setContainer(Package pack) {
		super.setContainer(pack);
		if (pack != null)
			pack.addInterface(this);
	}

	private final static String FORMAT = "public interface %s{%n}%n";

	public String toString() {
		return String.format(FORMAT, name);
	}
}
