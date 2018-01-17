package org.open.generate;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class SHACL {

	/**
	 * The namespace of the vocabulary as a string
	 */
	public static final String uri = "http://www.w3.org/ns/shacl#";

	protected static final Resource resource(String local) {
		return ResourceFactory.createResource(uri + local);
	}

	protected static final Property property(String local) {
		return ResourceFactory.createProperty(uri, local);
	}

	public static final Property targetClass = property("targetClass");
	public static final Property _class = property("class");
	public static final Property property = property("property");
	public static final Property path = property("path");
	public static final Property nodeKind = property("nodeKind");
	public static final Resource IRIOrLiteral = resource("IRIOrLiteral");
	public static final Resource Literal = resource("Literal");
	public static final Property maxCount = property("maxCount");;
}
