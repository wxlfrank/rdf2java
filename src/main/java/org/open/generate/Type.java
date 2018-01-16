package org.open.generate;

public abstract class Type {
	protected String name;

	protected Package container;

	public String getName() {
		return name;
	}

	public Package getContainer() {
		return container;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setContainer(Package pack) {
		if (pack != container) {
			this.container = pack;
		}
	}
}
