package org.open.structure;

import java.util.LinkedHashSet;
import java.util.Set;

public class Field {

	private Class container;

	private int max = Integer.MAX_VALUE;
	private int min = 0;

	private String name;

	private Set<Type> types;

	public Field(Class container, String name) {
		this.name = name;
		setContainer(container);
	}

	public Class getContainer() {
		return container;
	}

	public int getMax() {
		return max;
	}

	public String getName() {
		return name;
	}

	public Set<Type> getTypes() {
		if (types == null)
			types = new LinkedHashSet<Type>();
		return types;
	}

	public void setContainer(Class container) {
		this.container = container;
		if (container != null)
			this.container.addField(this);
	}

	public void setMax(int maxValue) {
		max = maxValue;
	}

	public void setType(Set<Type> types) {
		this.types = types;
	}

	public int getMin() {
		return min;
	}

	public void setMin(int min) {
		this.min = min;
	}
}
