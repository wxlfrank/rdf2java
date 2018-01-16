package org.open.generate;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

public abstract class TypeEx extends Binding {

	public TypeEx(Binding parent, RDFNode resource, Type target) {
		super(parent, resource, target);
	}

	public Type getType() {
		return (Type) target;
	}

	public Resource getSource() {
		return (Resource) source;
	}
}
