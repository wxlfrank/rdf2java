package org.open.structure;

public class Interface extends Inheritable {

	private final static String FORMAT = "public interface %s{%n}%n";

	public Interface() {
	}

	public Interface(Package pack, String name) {
		setName(name);
		setContainer(pack);
		if (pack != null)
			pack.addInterface(this);
	}

	public void setContainer(Package pack) {
		super.setContainer(pack);
		if (pack != null)
			pack.addInterface(this);
	}

	public String toString() {
		return String.format(FORMAT, name);
	}
}
