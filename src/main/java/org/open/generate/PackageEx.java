package org.open.generate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;

/**
 * The binding class to bind a model and its corresponding package
 * @author wxlfrank
 *
 */
public class PackageEx {

	
	public PackageEx(Package result, Model model) {
		this.pac = result;
		this.source = model;
	}

	private Map<String, TypeEx> annoymous = new HashMap<String, TypeEx>();
	private Package pac;
	private Model source;
	private Map<String, ClassEx> classes = new HashMap<String, ClassEx>();;

	public Model getModel() {
		return source;
	}

	public TypeEx getType(String id) {
		return annoymous.get(id);
	}


	public Package getPackage() {
		return pac;
	}

	public ClassEx getClass(String id) {
		return classes.get(id);
	}

	public ClassEx createClass(String local, Resource resource) {
		Class cls = new Class(pac, local);
		ClassEx result = new ClassEx(this, cls, resource);
		classes.put(local, result);
		return result;
	}

}
