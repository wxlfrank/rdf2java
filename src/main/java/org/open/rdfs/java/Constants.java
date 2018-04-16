package org.open.rdfs.java;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.open.rdfs.structure.ClassEx;
import org.open.rdfs.structure.PackageEx;

public class Constants {
	public static final Model DEFAULT_MODEL = ModelFactory.createDefaultModel();
	public static final PackageEx DEFAULT_PACKAGE = new PackageEx(null, "org.generate.utils", null);
	public static final String XMLSchema_URI = "http://www.w3.org/2001/XMLSchema#";
	public static final PackageEx XMLSchema_Package = new PackageEx(null, XMLSchema_URI, DEFAULT_MODEL);
	static {
		new ClassEx(XMLSchema_Package, "string", null);
		new ClassEx(XMLSchema_Package, "integer", null);
		new ClassEx(XMLSchema_Package, "boolean", null);
		new ClassEx(XMLSchema_Package, "dataTime", null);
		new ClassEx(XMLSchema_Package, "anyURI", null);
	}
}
