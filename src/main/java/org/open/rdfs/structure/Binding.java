package org.open.rdfs.structure;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public abstract class Binding {

	protected Map<String, ? extends Binding> contents = new HashMap<String, Binding>();
	protected Binding container;
	protected Object source;
	protected Object target;

	protected Set<Binding> equivalence = null;

	public Binding() {
	}

	public Binding(Binding parent, Object source, Object target) {
		this.source = source;
		this.target = target;
		if (parent != null)
			this.setContainer(parent);
	}

	public void addContent(Binding content) {
		this.getContents().put(content.getHashString(), content);
	}

	@SuppressWarnings("unchecked")
	public Map<String, Binding> getContents() {
		return (Map<String, Binding>) contents;
	}

	public String getHashString() {
		return "";
	}

	public Binding getContainer() {
		return container;
	}

	public Object getSource() {
		return source;
	}

	public Object getTarget() {
		return target;
	}

	public void setContainer(Binding container) {
		if (this.container != container) {
			this.container = container;
			if (container != null) {
				container.getContents().put(getHashString(), this);
			}
		}
	}

	public void setSource(Object source) {
		this.source = source;
	}

	public void setTarget(Object target) {
		this.target = target;
	}

	public Set<Binding> getEquivalence() {
		if (equivalence == null) {
			equivalence = new LinkedHashSet<Binding>();
		}
		return equivalence;
	}

	public void addEquivalence(Set<? extends Binding> fieldEx) {
		Set<Binding> equivalence = getEquivalence();
		for (Binding iter : fieldEx) {
			equivalence.add(iter);
			iter.getEquivalence().add(this);
		}
	}

	public void addEquivalence(Binding fieldEx) {
		Set<Binding> equivalence = getEquivalence();
		equivalence.add(fieldEx);
		fieldEx.getEquivalence().add(this);
	}

}
