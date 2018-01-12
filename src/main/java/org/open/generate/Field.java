package org.open.generate;

public class Field {

	

	private Class container;

	private String name;

	private Type type;

	public Field(Class container, String name) {
		this.name = name;
		setContainer(container);
	}

	public Class getContainer() {
		return container;
	}

	public Type getType() {
		return type;
	}

	public void setContainer(Class container) {
		this.container = container;
		if (container != null)
			this.container.addField(this);
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}
}
