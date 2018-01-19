package org.open.rdfs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

public class RDFSUtils {
	public static final String SHACL_NODESHAP = "http://www.w3.org/ns/shacl#NodeShape";

	private static Map<Model, Set<Resource>> classes = new HashMap<Model, Set<Resource>>();

	private static Map<Resource, Set<Resource>> domains = new HashMap<Resource, Set<Resource>>();

	private static Map<Model, Set<Resource>> nodeshapes = new HashMap<Model, Set<Resource>>();

	private static Map<Model, Set<Resource>> ontology = new HashMap<Model, Set<Resource>>();

	private static Map<Model, Set<Resource>> properties = new HashMap<Model, Set<Resource>>();

	private static Map<Resource, Set<Resource>> ranges = new HashMap<Resource, Set<Resource>>();
	private static Map<Resource, Set<Resource>> shaclProperties = new HashMap<Resource, Set<Resource>>();

	public static void analyseModel(Model model) {
		StmtIterator iterator = model.listStatements((Resource) null, RDF.type, (RDFNode) null);
		iterator.forEachRemaining(stmt -> {
			Resource resource = stmt.getSubject();
			RDFNode object = stmt.getObject();
			if (object.isURIResource()) {
				String uri = object.asResource().getURI();
				if (uri.equals(OWL.Ontology.getURI()) || uri.equals(RDFS.uri)) {
					Utils.putIntoMap(ontology, model, resource);
				} else if (uri.equals(RDFS.Class.getURI()) || uri.equals(OWL.Class.getURI()))
					Utils.putIntoMap(classes, model, resource);
				else if (uri.equals(RDF.Property.getURI()) || uri.equals(OWL.ObjectProperty.getURI())
						|| uri.equals(OWL.DatatypeProperty.getURI()))
					Utils.putIntoMap(properties, model, resource);
				else if (uri.equals(SHACL_NODESHAP))
					Utils.putIntoMap(nodeshapes, model, resource);
			}
		});
	}

	public static Set<Resource> getClasses(Model model) {
		return getOWLConcepts(model, classes);
	}

	/**
	 * Retrieve all the domains of a property
	 * 
	 * @param resource
	 *            the property
	 * @return all the domains
	 */
	public static Set<Resource> getDomains(Resource resource) {
		return getOWLProperties(resource, RDFS.domain, domains);
	}

	public static String getId(Resource resource) {
		return resource.isURIResource() ? resource.getURI() : resource.getId().toString();
	}

	public static List<Resource> getList(Resource resource) {
		List<Resource> result = new ArrayList<Resource>();
		Statement first = resource.getProperty(RDF.first);
		if (first != null) {
			result.add(first.getObject().asResource());
			Statement rest = resource.getProperty(RDF.rest);
			if (rest != null)
				result.addAll(getList(rest.getObject().asResource()));
		}
		return result;
	}

	public static Literal getMaxCount(Resource property) {
		return getPropertyAsLiteral(property, SHACL.maxCount);
	}

	public static Resource getNodeKind(Resource property) {
		return getPropertyAsResource(property, SHACL.nodeKind);
	}

	public static Set<Resource> getNodeShapes(Model model) {
		return getOWLConcepts(model, nodeshapes);
	}

	public static Set<Resource> getOntology(Model model) {
		return getOWLConcepts(model, ontology);
	}

	public static Set<Resource> getOWLConcepts(Model model, Map<Model, Set<Resource>> hash) {
		Set<Resource> result = hash.get(model);
		if (result != null)
			return result;
		Set<Resource> find = new HashSet<Resource>();
		hash.put(model, find);
		return find;
	}

	public static Set<Resource> getOWLProperties(Resource resource, Property property,
			Map<Resource, Set<Resource>> hash) {
		Set<Resource> result = hash.get(resource);
		if (result != null)
			return result;
		Set<Resource> results = getProperties(resource, property);
		hash.put(resource, results);
		return results;
	}

	public static Resource getPath(Resource property) {
		return getPropertyAsResource(property, SHACL.path);
	}

	public static Set<Resource> getProperties(Model model) {
		return getOWLConcepts(model, properties);
	}

	public static Set<Resource> getProperties(Resource resource, Property property) {
		Set<Resource> results = new HashSet<Resource>();
		resource.getModel().listStatements(resource, property, (RDFNode) null)
				.forEachRemaining(stm -> results.add(stm.getObject().asResource()));
		return results;
	}

	public static Set<Resource> getRanges(Resource resource) {
		return getOWLProperties(resource, RDFS.range, ranges);
	}

	public static Resource getShaclClass(Resource property) {
		return getPropertyAsResource(property, SHACL._class);
	}

	public static Set<Resource> getShaclProperties(Resource shape) {
		return getOWLProperties(shape, SHACL.property, shaclProperties);
	}

	/**
	 * retrieve all the super property that the property extends
	 * 
	 * @param resource
	 * @return
	 */
	public static Resource getSubProperty(Resource resource) {
		return getPropertyAsResource(resource, RDFS.subPropertyOf);
	}

	public static Resource getSuperClass(Resource resource) {
		return getPropertyAsResource(resource, RDFS.subClassOf);
	}

	public static Resource getTargetClass(Resource shape) {
		return getPropertyAsResource(shape, SHACL.targetClass);
	}

	public static Resource getType(Resource resource) {
		return getPropertyAsResource(resource, RDF.type);
	}

	public static Set<Resource> getTypes(Resource resource) {
		return getProperties(resource, RDF.type);
	}

	public static List<Resource> getUnionOf(Resource resource) {
		Statement stm = resource.getProperty(OWL.unionOf);
		if (stm != null) {
			return getList(stm.getObject().asResource());
		}
		return null;
	}

	public static boolean isClass(Set<Resource> types) {
		for (Resource type : types) {
			if (type.equals(RDFS.Class) || type.equals(OWL.Class))
				return true;
		}
		return false;
	}

	private static RDFNode getProperty(Resource resource, Property property) {
		if (resource == null)
			return null;
		Statement stm = resource.getProperty(property);
		if (stm == null)
			return null;
		return stm.getObject();
	}

	private static Literal getPropertyAsLiteral(Resource resource, Property property) {
		RDFNode value = getProperty(resource, property);
		return value != null ? value.asLiteral() : null;
	}

	private static Resource getPropertyAsResource(Resource resource, Property property) {
		RDFNode value = getProperty(resource, property);
		return value != null ? value.asResource() : null;
	}
}
