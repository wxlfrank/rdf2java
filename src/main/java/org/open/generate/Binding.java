package org.open.generate;

import java.util.HashMap;
import java.util.Map;

public abstract class Binding {

	protected Object source;
	protected Object target;
	protected Binding parent;
	private Map<String, ? extends Binding> children = new HashMap<String, Binding>();

	public Binding(Binding parent, Object source, Object target) {
		this.source = source;
		this.target = target;
		this.setParent(parent);
	}

	public Object getSource() {
		return source;
	}

	public void setSource(Object source) {
		this.source = source;
	}

	public Object getTarget() {
		return target;
	}

	public void setTarget(Object target) {
		this.target = target;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Binding> getChildren() {
		return (Map<String, Binding>) children;
	}

	public void addChild(Binding child) {
		this.getChildren().put(child.getHashString(), child);
	}

	public Binding getParent() {
		return parent;
	}

	public void setParent(Binding parent) {
		if (this.parent != parent) {
			this.parent = parent;
			if (parent != null) {
				parent.getChildren().put(getHashString(), this);
			}
		}
	}

	public abstract String getHashString();
}
