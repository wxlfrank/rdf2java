package org.open.generate;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class Constants {
	public static final String XMLSchema_URI = "http://www.w3.org/2001/XMLSchema#";
	public static final Model DEFAULT_MODEL = ModelFactory.createDefaultModel();
	public static final PackageEx XMLSchema_Package = new PackageEx(new Package(XMLSchema_URI), DEFAULT_MODEL);
	static {
		XMLSchema_Package.createClass("string", null);
		XMLSchema_Package.createClass("integer", null);
		XMLSchema_Package.createClass("boolean", null);
		XMLSchema_Package.createClass("dataTime", null);
//		XMLSchema_Package.createClass("nonNegativeInteger", null);
	}
}
