package org.open.rdfs;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class SHACL {

	public static final Property clazz = property("class");
	public static final Property node = property("node");

	public static final Resource IRIOrLiteral = resource("IRIOrLiteral");
	public static final Resource NodeShape = resource("NodeShape");

	public static final Resource Literal = resource("Literal");

	public static final Property maxCount = property("maxCount");
	public static final Property minCount = property("minCount");
	public static final Property nodeKind = property("nodeKind");
	public static final Property path = property("path");
	public static final Property datatype = property("datatype");
	public static final Property property = property("property");
	public static final Property or = property("or");
	public static final Property targetClass = property("targetClass");
	/**
	 * The namespace of the vocabulary as a string
	 */
	public static final String uri = "http://www.w3.org/ns/shacl#";
	protected static final Property property(String local) {
		return ResourceFactory.createProperty(uri, local);
	}
	protected static final Resource resource(String local) {
		return ResourceFactory.createResource(uri + local);
	};
}
