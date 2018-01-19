package org.open.rdfs;

import java.util.HashMap;
import java.util.Map;

public abstract class Binding {

	private Map<String, ? extends Binding> children = new HashMap<String, Binding>();
	protected Binding parent;
	protected Object source;
	protected Object target;

	public Binding(Binding parent, Object source, Object target) {
		this.source = source;
		this.target = target;
		this.setParent(parent);
	}

	public void addChild(Binding child) {
		this.getChildren().put(child.getHashString(), child);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Binding> getChildren() {
		return (Map<String, Binding>) children;
	}

	public abstract String getHashString();

	public Binding getParent() {
		return parent;
	}

	public Object getSource() {
		return source;
	}

	public Object getTarget() {
		return target;
	}

	public void setParent(Binding parent) {
		if (this.parent != parent) {
			this.parent = parent;
			if (parent != null) {
				parent.getChildren().put(getHashString(), this);
			}
		}
	}

	public void setSource(Object source) {
		this.source = source;
	}

	public void setTarget(Object target) {
		this.target = target;
	}
}
