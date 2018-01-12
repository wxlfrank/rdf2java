package org.open.generate;

import org.apache.jena.rdf.model.RDFNode;

public class TypeEx {

	public TypeEx(Type cls, RDFNode resource) {
		this.setType(cls);
		this.setSource(resource);
	}
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}
	public RDFNode getSource() {
		return source;
	}
	public void setSource(RDFNode source) {
		this.source = source;
	}
	protected Type type;
	private RDFNode source;
}
