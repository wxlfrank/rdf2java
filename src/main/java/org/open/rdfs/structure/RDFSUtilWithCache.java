package org.open.rdfs.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.open.Util;
import org.open.rdfs.RDFSUtil;
import org.open.rdfs.SHACL;

public class RDFSUtilWithCache extends RDFSUtil {

	public static final RDFSUtilWithCache INSTANCE = new RDFSUtilWithCache();

	public static final Set<Resource> EMPTY_SET = new LinkedHashSet<Resource>();

	public static Set<Resource> getProperties(Resource resource, Property property,
			Map<Resource, Set<Resource>> cache) {
		Set<Resource> result = cache.get(resource);
		if (result != null)
			return result;
		Set<Resource> results = RDFSUtil.getProperties(resource, property);
		cache.put(resource, results);
		return results;
	}

	private Map<Model, Set<Resource>> model2classes = new HashMap<Model, Set<Resource>>();
	private Map<Model, Set<Resource>> model2datatypes = new HashMap<Model, Set<Resource>>();
	private Map<Resource, Set<Resource>> property2domains = new HashMap<Resource, Set<Resource>>();
	private Map<Resource, Set<Resource>> domain2properties = new HashMap<Resource, Set<Resource>>();

	private Map<Resource, Set<Resource>> property2ranges = new HashMap<Resource, Set<Resource>>();

	private Map<Resource, Set<Resource>> class2parents = new HashMap<Resource, Set<Resource>>();

	private Map<Model, Set<Resource>> model2shapes = new HashMap<Model, Set<Resource>>();

	private Map<Model, Set<Resource>> model2schemas = new HashMap<Model, Set<Resource>>();

	private Map<Model, Set<Resource>> model2properties = new HashMap<Model, Set<Resource>>();

	private Map<Resource, Set<Resource>> shaclProperties = new HashMap<Resource, Set<Resource>>();
	private Map<Resource, Set<Resource>> property2properties = new HashMap<Resource, Set<Resource>>();

	private Map<Resource, Set<Resource>> shape2classes = new HashMap<Resource, Set<Resource>>();
	private Map<Resource, Set<Resource>> class2shapes = new HashMap<Resource, Set<Resource>>();

	Map<Model, String> model2namespace = new HashMap<Model, String>();

	private Set<Resource> shapeHandled = new LinkedHashSet<Resource>();

	public Set<Resource> getClasses(Model model) {
		return Util.getFromCache(model2classes, model);
	}
	public Set<Resource> getDataTypes(Model model) {
		return Util.getFromCache(model2datatypes, model);
	}

	public Set<Resource> getClassesOfShape(Resource source) {
		return Util.getFromCache(shape2classes, source);
	}

	public Set<Resource> getDomainsOfProperty(Resource property) {
		return RDFSUtil.getProperties(property, RDFS.domain);
	}

	/**
	 * Retrieve all the domains of a property
	 * 
	 * @param property
	 *            the property
	 * @return all the domains
	 */
	public Set<Resource> getDomainsOfPropertyWithCache(Resource property) {
		return getProperties(property, RDFS.domain, property2domains);
	}

	public String getFromModel2Namespace(Model model) {
		return model2namespace.get(model);
	}

	public Set<Resource> getNodeShapes(Model model) {
		return Util.getFromCache(model2shapes, model);
	}

	public Set<Resource> getOntology(Model model) {
		return Util.getFromCache(model2schemas, model);
	}

	public Set<Resource> getProperties(Model model) {
		return Util.getFromCache(model2properties, model);
	}

	public Set<Resource> getPropertiesOfDomain(Resource domain) {
		return Util.getFromCache(domain2properties, domain);
	}

	public Set<Resource> getRangesOfProperty(Resource resource) {
		return RDFSUtil.getProperties(resource, RDFS.range);
	}

	public Set<Resource> getRangesOfPropertyWithCache(Resource resource) {
		return getProperties(resource, RDFS.range, property2ranges);
	}

	/**
	 * <ol>
	 * <li>If the {@code model} contains a resource whose type is
	 * {@link org.apache.jena.vocabulary.OWL#Ontology OWL.Ontology}, return the
	 * namespace of the resource</li>
	 * <li>If the {@code model} contains a resource whose type is
	 * {@link org.apache.jena.vocabulary.RDFS#uri RDFS.uri}, return the namespace of
	 * the resource</li>
	 * <li>find a resource whose type is
	 * {@link org.apache.jena.vocabulary.RDFS#Class RDFS.Class}, return the
	 * namespace of the resource</li>
	 * </ol>
	 * 
	 * @param model
	 *            the model to be analyse
	 * @return the real namespace of a {@code model}
	 */
	public String getRealNamespace(Model model) {
		if (model != null) {
			String namespace = model2namespace.get(model);
			if (namespace != null)
				return namespace;
			List<String> uris = new ArrayList<String>();
			getOntology(model).forEach(ontology -> {
				uris.add(ontology.getURI());
			});
			if (uris.isEmpty()) {
				for (Resource clazz : getClasses(model)) {
					if (clazz.getURI() != null) {
						uris.add(clazz.getNameSpace());
						break;
					}
				}
			}
			if (uris.isEmpty()) {
				for (Resource property : getProperties(model)) {
					if (property.getURI() != null) {
						uris.add(property.getNameSpace());
						break;
					}
				}
			}
			if (!uris.isEmpty())
				namespace = RDFSUtil.unifyNS(uris.get(0));
			model2namespace.put(model, namespace);
			return namespace;
		}
		return null;
	}

	public Set<Resource> getShaclProperties(Resource shape) {
		return getProperties(shape, SHACL.property, shaclProperties);
	}

	public Set<Resource> getShapesOfClass(Resource source) {
		Set<Resource> result = class2shapes.get(source);
		if (result == null) {
			result = EMPTY_SET;
		}
		return result;
	}

	public Set<Resource> getShapesOfModel(Model model) {
		return model2shapes.get(model);
	}

	public Set<Resource> getSuperClass(Resource resource) {
		return getProperties(resource, RDFS.subClassOf, class2parents);
	}

	public boolean isShapeHandled(Resource parent) {
		return shapeHandled.contains(parent);
	}

	public void putModel2Namespace(Model model, String namespace) {
		model2namespace.put(model, namespace);
	}

	public void setShapeHandled(Resource clazz) {
		shapeHandled.add(clazz);
	}

	/**
	 * Traverse model and hash elements needed
	 * 
	 * @param model
	 */
	public void traverseModel(Model model) {
		StmtIterator iterator = model.listStatements((Resource) null, RDF.type, (RDFNode) null);
		iterator.forEachRemaining(stmt -> {
			Resource subject = stmt.getSubject();
			RDFNode object = stmt.getObject();
			if (object.isURIResource()) {
				String uri = object.asResource().getURI();
				// subject is an ontology or rdfs schema
				if (uri.equals(OWL.Ontology.getURI()) || uri.equals(RDFS.uri))
					Util.putIntoCache(model2schemas, model, subject);
				// subject is a class
				else if (uri.equals(RDFS.Class.getURI()) || uri.equals(OWL.Class.getURI())) {
					Set<Resource> search = Util.getFromCache(model2datatypes, model);
					if (!search.contains(subject))
						Util.putIntoCache(model2classes, model, subject);
				} else if (uri.equals(RDFS.Datatype.getURI())) {
					Util.putIntoCache(model2datatypes, model, subject);
					Set<Resource> search = Util.getFromCache(model2classes, model);
					if (search.contains(subject))
						search.remove(subject);
				}
				// subject is a property
				else if (uri.equals(RDF.Property.getURI()) || uri.equals(OWL.ObjectProperty.getURI())
						|| uri.equals(OWL.DatatypeProperty.getURI()))
					Util.putIntoCache(model2properties, model, subject);
				// subject is a shacl shape
				// currently just consider shacl node shape
				else if (uri.equals(SHACL.NodeShape.getURI())) {
					Resource shape = subject;
					Util.putIntoCache(model2shapes, model, shape);
					Resource targetClass = RDFSUtil.getTargetClassOfNodeShape(shape);
					if (targetClass != null) {
						Util.putIntoCache(class2shapes, targetClass, shape);
						Util.putIntoCache(shape2classes, shape, targetClass);
					}
				}
				// else if(uri.equals(OWL.equivalentClass.getURI())) {
				// Util.putIntoCache(class2classes, subject, (Resource)object);
				// }
				// else if(uri.equals(OWL.equivalentProperty.getURI())) {
				// Util.putIntoCache(property2properties, subject, (Resource)object);
				// }
			}
		});
		getProperties(model).forEach(property -> {
			getDomainsOfProperty(property).forEach(eachDomain -> {
				Util.putIntoCache(domain2properties, eachDomain, property);
				Util.putIntoCache(property2domains, property, eachDomain);
			});
			getRangesOfProperty(property).forEach(range -> {
				Util.putIntoCache(property2ranges, property, range);
			});
		});

	}

	private Map<Resource, Set<ClassEx>> nodeshape2classes = new HashMap<Resource, Set<ClassEx>>();

	public Set<ClassEx> getShapeTypes(Resource shape) {
		return nodeshape2classes.get(shape);
	}

	public void setShapeTypes(Resource shape, Set<ClassEx> classes) {
		nodeshape2classes.put(shape, classes);
	}

	public Set<Resource> getEquivalences(Resource rdfsProperty) {
		Set<Resource> value = property2properties.get(rdfsProperty);
		if (value == null) {
			value = RDFSUtil.getProperties(rdfsProperty, OWL.equivalentProperty);
			property2properties.put(rdfsProperty, value);
		}
		return value;
	}

}
