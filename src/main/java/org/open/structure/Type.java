package org.open.structure;

public abstract class Type {
	protected Package container;

	protected String name;

	public Package getContainer() {
		return container;
	}

	public String getName() {
		return name;
	}

	public void setContainer(Package pack) {
		if (pack != container) {
			this.container = pack;
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	public String toString() {
		return (container != null ? container.getName() : "") + name;
	}

	public boolean isTypeOf(Type baseType) {
		if (baseType == this)
			return true;
		return false;
	}
}
