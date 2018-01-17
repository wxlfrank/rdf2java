package org.open.generate;

import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
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

	private static Map<String, String> uri2pac = new HashMap<String, String>();

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

	public static Set<Resource> getNodeShapes(Model model) {
		return getOWLConcepts(model, nodeshapes);
	}

	public static Set<Resource> getOntology(Model model) {
		return getOWLConcepts(model, ontology);
	}

	public static String getPackageName(String ns) {
		if (ns.isEmpty())
			return ns;
		String result = uri2pac.get(ns);
		if (result != null)
			return result;
		int index = ns.indexOf("//");
		String newNS = index == -1 ? ns : ns.substring(index + 2);
		index = newNS.indexOf("www.");
		newNS = index == -1 ? newNS : newNS.substring(index + 4);
		newNS = newNS.replaceAll("-", "_").replaceAll("#", "");
		String domain = null, last = null;
		index = newNS.indexOf("/");
		if (index == -1) {
			domain = newNS;
		} else {
			domain = newNS.substring(0, index);
			last = newNS.substring(index + 1);
		}
		List<String> domains = Arrays.asList(domain.split("\\."));
		Collections.reverse(domains);
		domain = String.join(".", domains);
		if (last != null) {
			if (last.endsWith("/"))
				last = last.substring(0, last.length() - 1);
			last = last.replaceAll("/", "_").replaceAll("\\.", "_");
			if (!last.isEmpty()) {
				if (Character.isDigit(last.charAt(0)))
					last = "_" + last;
				domain += "." + last;
			}
		}
		uri2pac.put(ns, domain);
		return domain;
	}

	public static Set<Resource> getProperties(Model model) {
		return getOWLConcepts(model, properties);
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

	public static Set<Resource> getProperties(Resource resource, Property property) {
		Set<Resource> results = new HashSet<Resource>();
		resource.getModel().listStatements(resource, property, (RDFNode) null)
				.forEachRemaining(stm -> results.add(stm.getObject().asResource()));
		return results;
	}

	public static Set<Resource> getRanges(Resource resource) {
		return getOWLProperties(resource, RDFS.range, ranges);
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
	public static String getRealNamespace(Model model) {
		if (model != null) {
			List<String> uris = new ArrayList<String>();
			getOntology(model).forEach(resource -> {
				uris.add(resource.getURI());
			});
			if (uris.isEmpty()) {
				for (Resource subject : getClasses(model)) {
					if (subject.getURI() != null) {
						uris.add(subject.getNameSpace());
						break;
					}
				}
			}
			if (uris.isEmpty()) {
				for (Resource subject : getProperties(model)) {
					if (subject.getURI() != null) {
						uris.add(subject.getNameSpace());
						break;
					}
				}
			}
			return unifyNS(uris.get(0));
		}
		return null;
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

	private static RDFNode getProperty(Resource resource, Property property) {
		if (resource == null)
			return null;
		Statement stm = resource.getProperty(property);
		if (stm == null)
			return null;
		return stm.getObject();
	}
	private static Resource getPropertyAsResource(Resource resource, Property property) {
		RDFNode value = getProperty(resource, property);
		return value != null ? value.asResource() : null;
	}
	private static Literal getPropertyAsLiteral(Resource resource, Property property) {
		RDFNode value = getProperty(resource, property);
		return value != null ? value.asLiteral() : null;
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

	/**
	 * Read a model corresponding to a url<br>
	 * If the url is not <a href="http://www.w3.org/2001/XMLSchema#">XMLSchema</a>
	 * and not <a href="http://schema.org/#">schema.org</a> (Temporary
	 * handling, @TODO handle these two cases in the future)<br>
	 * <ol>
	 * <li>read the model using the {@code url}</li>
	 * <li>find the real namespace of the model</li>
	 * <li>write the model in TURTLE format locally</li>
	 * </ol>
	 * 
	 * @param url
	 *            the url for the model to be read
	 * @param format
	 *            the format of the model to be read
	 * @return an {@link Entry} entry where the key is the real namespace of the
	 *         model and the value is the newly read model
	 */
	public static Map.Entry<String, Model> readWriteModel(String url, String format) {
		Model model = ModelFactory.createDefaultModel();
		String ns = url;
		if (!url.equals("http://www.w3.org/2001/XMLSchema#") && !url.equals("http://schema.org/#") && !url.equals("http://spdx.org/rdf/terms#")) {
			System.out.println(url);
			model = model.read(url, format);
			// base = (OntModel) base.union(model);
			RDFSUtils.analyseModel(model);
			ns = RDFSUtils.getRealNamespace(model);
			if (url.equals(ns)) {
				try {
					model.write(new FileWriter("ontology/" + RDFSUtils.getPackageName(ns)), "TURTLE");
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return new AbstractMap.SimpleEntry<String, Model>(ns, model);
	}

	/**
	 * unify a namespace if a namespace has not "#" at the end
	 * 
	 * @param namespace
	 * @return the unified namespace
	 */
	public static String unifyNS(String namespace) {
		if (namespace == null)
			return namespace;
		return namespace.endsWith("#") ? namespace : namespace + "#";
	}

	public static Resource getTargetClass(Resource shape) {
		return getPropertyAsResource(shape, SHACL.targetClass);
	}

	public static Set<Resource> getShaclProperties(Resource shape) {
		return getOWLProperties(shape, SHACL.property, shaclProperties);
	}

	public static Resource getPath(Resource property) {
		return getPropertyAsResource(property, SHACL.path);
	}

	public static Resource getNodeKind(Resource property) {
		return getPropertyAsResource(property, SHACL.nodeKind);
	}

	public static Resource getShaclClass(Resource property) {
		return getPropertyAsResource(property, SHACL._class);
	}

	public static Literal getMaxCount(Resource property) {
		return getPropertyAsLiteral(property, SHACL.maxCount);
	}
}
