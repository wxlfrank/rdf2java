package org.open.rdfs.structure;

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.open.structure.Type;

public abstract class TypeEx extends Binding {

	public TypeEx(Binding parent, RDFNode resource, Type target) {
		super(parent, resource, target);
	}

	public Resource getSource() {
		return (Resource) source;
	}

	public Type getType() {
		return (Type) target;
	}
}
