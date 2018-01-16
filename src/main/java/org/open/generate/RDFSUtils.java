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
	private static Map<Resource, Set<Resource>> domains = new HashMap<Resource, Set<Resource>>();

	public static Set<Resource> getDomains(Resource resource) {
		Set<Resource> result = domains.get(resource);
		if (result != null)
			return result;
		Set<Resource> results = new HashSet<Resource>();
		resource.getModel().listStatements(resource, RDFS.domain, (RDFNode) null).forEachRemaining(stm -> {
			results.add(stm.getObject().asResource());
		});
		domains.put(resource, results);
		return results;
	}

	private static Map<Resource, Set<Resource>> ranges = new HashMap<Resource, Set<Resource>>();

	public static Set<Resource> getRanges(Resource resource) {
		Set<Resource> result = ranges.get(resource);
		if (result != null)
			return result;
		Set<Resource> results = new HashSet<Resource>();
		StmtIterator iterator = resource.getModel().listStatements(resource, RDFS.range, (RDFNode) null);
		while (iterator.hasNext()) {
			results.add(iterator.next().getObject().asResource());
		}
		ranges.put(resource, results);
		return results;
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

	private static Map<Model, Set<Resource>> properties = new HashMap<Model, Set<Resource>>();

	public static Set<Resource> getProperties(Model model) {
		Set<Resource> result = properties.get(model);
		if (result != null)
			return result;
		Set<Resource> find = new HashSet<Resource>();
		properties.put(model, find);
		return find;
	}

	private static Map<Model, Set<Resource>> classes = new HashMap<Model, Set<Resource>>();

	public static Set<Resource> getClasses(Model model) {
		Set<Resource> result = classes.get(model);
		if (result != null)
			return result;
		Set<Resource> find = new HashSet<Resource>();
		classes.put(model, find);
		return find;
	}

	private static Map<Model, Set<Resource>> ontology = new HashMap<Model, Set<Resource>>();

	public static Set<Resource> getOntology(Model model) {
		Set<Resource> result = ontology.get(model);
		if (result != null)
			return result;
		Set<Resource> find = new HashSet<Resource>();
		model.listSubjectsWithProperty(RDF.type, OWL.Ontology).forEachRemaining(resource -> find.add(resource));
		model.listSubjectsWithProperty(RDF.type, RDFS.uri).forEachRemaining(resource -> find.add(resource));
		ontology.put(model, find);
		return find;
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

	private static Map<String, String> uri2pac = new HashMap<String, String>();

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
		if (!url.equals("http://www.w3.org/2001/XMLSchema#") && !url.equals("http://schema.org/#")) {
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

	public static Resource getSuperClass(Resource resource) {
		Statement stmt = resource.getProperty(RDFS.subClassOf);
		if (stmt != null)
			return stmt.getObject().asResource();
		return null;
	}

	public static Resource getSubProperty(Resource resource) {
		if (resource == null)
			return null;
		Statement stm = resource.getProperty(RDFS.subPropertyOf);
		if (stm == null)
			return null;
		return stm.getObject().asResource();
	}

	public static Resource getType(Resource resource) {
		Statement obj = resource.getProperty(RDF.type);
		if (obj != null)
			return obj.getObject().asResource();
		return null;
	}

	public static Set<Resource> getProperties(Resource resource, Property property) {
		Set<Resource> results = new HashSet<Resource>();
		StmtIterator iterator = resource.getModel().listStatements(resource, property, (RDFNode) null);
		while (iterator.hasNext()) {
			results.add(iterator.next().getObject().asResource());
		}
		return results;
	}

	public static Set<Resource> getTypes(Resource resource) {
		return getProperties(resource, RDF.type);
	}

	public static boolean isClass(Set<Resource> types) {
		for (Resource type : types) {
			if (type.equals(RDFS.Class) || type.equals(OWL.Class))
				return true;
		}
		return false;
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

	public static List<Resource> getUnionOf(Resource resource) {
		Statement stm = resource.getProperty(OWL.unionOf);
		if (stm != null) {
			return getList(stm.getObject().asResource());
		}
		return null;
	}

	private static Map<Model, Set<Resource>> nodeshapes = new HashMap<Model, Set<Resource>>();

	public static Set<Resource> getNodeShapes(Model model) {
		Set<Resource> result = nodeshapes.get(model);
		if (result != null)
			return result;
		Set<Resource> find = new HashSet<Resource>();
		nodeshapes.put(model, find);
		return find;
	}

	public static final String SHACL_NODESHAP = "http://www.w3.org/ns/shacl#NodeShape";

	public static void analyseModel(Model model) {
		StmtIterator iterator = model.listStatements((Resource) null, RDF.type, (RDFNode) null);
		iterator.forEachRemaining(stmt -> {
			Resource resource = stmt.getSubject();
			RDFNode object = stmt.getObject();
			if (object.isURIResource()) {
				String uri = object.asResource().getURI();
				if (uri.equals(RDFS.Class.getURI()) || uri.equals(OWL.Class.getURI()))
					Utils.putIntoMap(classes, model, resource);
				else if (uri.equals(RDF.Property.getURI()) || uri.equals(OWL.ObjectProperty.getURI())
						|| uri.equals(OWL.DatatypeProperty.getURI()))
					Utils.putIntoMap(properties, model, resource);
				else if (uri.equals(SHACL_NODESHAP))
					Utils.putIntoMap(nodeshapes, model, resource);
			}
		});
	}
}
