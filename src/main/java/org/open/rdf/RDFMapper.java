package org.open.rdf;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.open.Util;
import org.open.rdf.annotation.RDF;
import org.open.rdf.annotation.RDFLiteral;
import org.open.rdfs.RDFSUtil;

public class RDFMapper {

	/**
	 * The hash table that maps a java field to a RDF property
	 */
	private Map<Field, Property> field2rdfProperty = new HashMap<Field, Property>();
	/**
	 * The hash table that maps a resource to a java object, which is stored by the
	 * object's class
	 */
	private final Map<Class<?>, Map<RDFNode, Object>> java_class2rdf_resource2java_object = new HashMap<Class<?>, Map<RDFNode, Object>>();

	private RDFMapperConfig config = new RDFMapperConfig();

	// the hash table which map a java runtime class to a RDF resource which is used
	// to specify the class of a RDF instance
	private Map<Class<?>, Object> javaclass2rdftype = new HashMap<Class<?>, Object>();

	/**
	 * The hash table that maps the RDF resource which is a RDF:Class to its
	 * corresponding java class.
	 */
	private final Map<Resource, Class<?>> rdf_class2java_class = new HashMap<Resource, Class<?>>();

	/**
	 * The java objects that are translated from RDF resources
	 */
	private final Set<Object> read_objects = new LinkedHashSet<Object>();

	/**
	 * The java objects that are translated from RDF resources and are the root of
	 * the read_objects
	 */
	private final Set<Object> read_topmost = new LinkedHashSet<Object>();

	/**
	 * The utilities to retrieve java reflective information, e.g, Class, Field,
	 * Method, etc.
	 */
	private JavaReflectUtils util = new JavaReflectUtils();

	/**
	 * Find the RDF class for a java runtime class {@code java_runtime_class}
	 * 
	 * @param rdf_model
	 *            the RDF model
	 * @param java_runtime_class
	 *            the java runtime class
	 * @return the RDF class
	 */
	public Object findRDFClass(Model rdf_model, Class<?> java_runtime_class) {
		Object result = javaclass2rdftype.get(java_runtime_class);
		if (result == null) {
			RDF annotation = java_runtime_class.getAnnotation(RDF.class);
			if (annotation != null) {
				result = rdf_model.createResource(annotation.namespace() + annotation.local());
			} else {
				RDFLiteral literal = java_runtime_class.getAnnotation(RDFLiteral.class);
				result = literal.namespace() + literal.local();
			}
			if (null == result)
				return null;
			javaclass2rdftype.put(java_runtime_class, result);
		}
		return result;
	}

	/**
	 * Translate a RDF model to java objects
	 * 
	 * @param model
	 *            the RDF model
	 * @return the topmost java objects
	 */
	public Object read(Model model) {
		StmtIterator stmts = model.listStatements();
		boolean newCreated = false;
		while (stmts.hasNext()) {
			Statement stmt = stmts.next();
			// translate subject
			Resource rdf_subject = stmt.getSubject();
			Object java_subject = rdf2java.get(rdf_subject);
			newCreated = java_subject == null;
			if (java_subject == null) {
				java_subject = toJavaObject(rdf_subject);
			}
			if (java_subject == null)
				continue;
			if (newCreated) {
				read_topmost.add(java_subject);
			}

			// translate object
			RDFNode rdf_object = stmt.getObject();
			Object java_object = rdf2java.get(rdf_object);
			newCreated = java_object == null;
			if (newCreated) {
				java_object = toJavaObject(rdf_object);
			}
			if (java_object != null) {
				if (newCreated) {
					read_objects.add(java_object);
				} else {
					if (read_topmost.contains(java_object))
						read_topmost.remove(java_object);
				}
				Resource resource = stmt.getPredicate();
				String uri = RDFSUtil.unifyNS(resource.getNameSpace()) + resource.getLocalName();
				Object accessor = util.getJavaFieldSetter(java_subject.getClass()).get(uri);
				if(accessor == null) {
					
					String warning = model.getNsURIPrefix(resource.getNameSpace()) + ":" + resource.getLocalName() + " is not defined in epos-dcat-ap_shapes.ttl";
					Resource type = RDFSUtil.getType(rdf_subject);
					warning += ". It should defined in " + model.getNsURIPrefix(type.getNameSpace()) + ":" + type.getLocalName() + " or its superclass.";
					if(Util.addWarning(warning)) {
//						System.out.println("--------------------------");
//						model.getNsPrefixMap().forEach((a, b)-> System.out.println(a + "-->" + b));;
						System.out.println(warning);
					}
//					System.out.println("There is no right accessor for the property " + uri + " in the definition of the class " + java_subject.getClass().getName());
				}
				util.setProperty(accessor, java_subject, java_object);
			}

		}
		if (read_topmost.isEmpty())
			return read_objects.isEmpty() ? null : read_objects;
		return read_topmost.size() == 1 ? read_topmost.iterator().next() : read_topmost;
	}

	/**
	 * Transform an java object {@code java_object} to a RDF resource in a model
	 * {@code model}
	 * 
	 * @param rdf_model
	 *            the RDF model to be created
	 * @param java_object
	 *            the java object to be transformed
	 * @return the RDF resource to be created
	 */
	public RDFNode toRDFResource(Model rdf_model, Object java_object) {
		// if obj is a collection, transform obj to a list of resources
		if (java_object instanceof Collection<?>) {
			List<RDFNode> results = new ArrayList<RDFNode>();
			for (Object iter : (Collection<?>) java_object) {
				results.add(toRDFResource(rdf_model, iter));
			}
			if (!results.isEmpty())
				return rdf_model.createList(results.iterator());
			return null;
		}
		// if obj is a single object, transform it to a single resource
		Object type = findRDFClass(rdf_model, java_object.getClass());
		if (type instanceof Resource) {
			Resource resource = rdf_model.createResource();
			resource.addProperty(org.apache.jena.vocabulary.RDF.type, (Resource) type);
			toRDFStatement(rdf_model, resource, java_object);
			return resource;
		}
		if (type instanceof String) {
			return rdf_model.createTypedLiteral(java_object.toString(), (String) type);
		}
		return null;
	}

	/**
	 * Translate each property of java object {@code java_object} into a RDF
	 * statement
	 * 
	 * @param model
	 *            the model containing the created RDF statement
	 * @param rdf_subject
	 *            the RDF subject of the created RDF statement
	 * @param java_object
	 *            the java object
	 */
	public void toRDFStatement(Model model, Resource rdf_subject, Object java_object) {
		java.lang.Class<?> java_class = java_object.getClass();
		Map<Field, Object> fieldAccessor = util.getJavaFieldGetter(java_class);
		for (Entry<Field, Object> field_accessor : fieldAccessor.entrySet()) {
			Field field = field_accessor.getKey();
			Object java_accessor = field_accessor.getValue();
			Object java_value = util.getJavaFieldValue(java_class, java_accessor, java_object);
			if (java_value == null || (java_value instanceof Collection<?> && ((Collection<?>) java_value).isEmpty()))
				continue;
			Property rdf_property = findRDFProperty(model, field);
			if (rdf_property == null)
				continue;
			RDFNode node = toRDFResource(model, java_value);
			if (node instanceof Literal) {
				rdf_subject.addLiteral(rdf_property, (Literal) node);
			} else if (node instanceof Resource)
				rdf_subject.addProperty(rdf_property, (Resource) node);
		}
	}

	/**
	 * Write a java object {@code java_object} to a RDF model
	 * 
	 * @param java_object
	 *            the java object
	 * @return the RDF model to be created
	 */
	public Model write(Object java_object) {
		Model model = ModelFactory.createDefaultModel();
		// transform obj to a resource in model

		toRDFResource(model, java_object);
		return model;
	}

	/**
	 * Find the RDF property for the java {@code field}
	 * 
	 * @param model
	 * @param field
	 * @return the RDF property
	 */
	private Property findRDFProperty(Model model, Field field) {
		Property result = field2rdfProperty.get(field);
		if (result == null) {
			RDF[] annotations = field.getAnnotationsByType(RDF.class);
			if (annotations.length == 0)
				return null;
			RDF annotation = annotations[0];
			result = model.createProperty(annotation .namespace(), annotation.local());
			field2rdfProperty.put(field, result);
		}
		return result;
	}

	/**
	 * Get the java class for the RDF class {@code}
	 * 
	 * @param rdf_class
	 *            the RDF class
	 * @return the corresponding java class
	 */
	private Class<?> getClassForURI(Resource rdf_class) {
		String ns = rdf_class.getNameSpace(), local = rdf_class.getLocalName();
		Class<?> result = rdf_class2java_class.get(rdf_class);
		if (result == null) {
			try {
				result = Class.forName(config.getPackageName(ns) + "." + local);
				rdf_class2java_class.put(rdf_class, result);
			} catch (ClassNotFoundException e) {
				return null;
			}
		}
		return result;
	}

	/**
	 * Get the java object which is instance of a java class {@code java_class}
	 * corresponding to a RDF resource
	 * 
	 * @param rdf_resource
	 *            the RDF resource
	 * @param java_class
	 *            the java class
	 * @return the java object
	 */
	private Object getJavaObject(RDFNode rdf_resource, Class<?> java_class) {
		Object result = Util.getFromCache(java_class2rdf_resource2java_object, java_class, rdf_resource);
		if (result == null) {
			try {
				result = java_class.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				return null;
			}
			Util.putIntoCache(java_class2rdf_resource2java_object, java_class, rdf_resource, result);
		}
		return result;
	}

	Map<RDFNode, Object> rdf2java = new HashMap<RDFNode, Object>();

	/**
	 * Translate a RDF resource to a java object
	 * 
	 * @param rdf_node
	 *            the RDF resource
	 * @return the translated java object and whether the object is newly created or
	 *         not
	 */
	private Object toJavaObject(RDFNode rdf_node) {
		Object result = rdf2java.get(rdf_node);
		if (result != null)
			return result;
		if (rdf_node instanceof Resource) {
			Resource resource = (Resource) rdf_node;
			List<Resource> list = RDFSUtil.getList(resource);
			if (list == null) {
				Resource type = RDFSUtil.getType(rdf_node);
				if (type != null) {

					Class<?> cls = getClassForURI(type);
					if (cls == null)
						return null;
					result = getJavaObject(rdf_node, cls);
					rdf2java.put(rdf_node, result);
					return result;
				}
			} else {
				List<Object> results = new ArrayList<Object>();
				for (Resource iter : list) {
					Object cur = toJavaObject(iter);
					if (read_topmost.contains(cur)) {
						read_topmost.remove(cur);
						read_objects.add(cur);
					}

					results.add(cur);
				}
				rdf2java.put(rdf_node, results);
				return results;
			}
		} else if (rdf_node instanceof Literal) {
			Literal literal = (Literal) rdf_node;
			RDFDatatype type = literal.getDatatype();
			Object value = literal.getValue();
			if (type == XSDDatatype.XSDstring) {
				return new org.w3._2001_XMLSchema.String(value);
			} else if (type == XSDDatatype.XSDdate)
				return new org.w3._2001_XMLSchema.Date(value);
			else if (type == XSDDatatype.XSDboolean)
				return new org.w3._2001_XMLSchema.Boolean(value);
			else if (type == XSDDatatype.XSDint)
				return new org.w3._2001_XMLSchema.Integer(value);
			else if (type == XSDDatatype.XSDfloat)
				return new org.w3._2001_XMLSchema.Float(value);
			else if (type == XSDDatatype.XSDdouble)
				return new org.w3._2001_XMLSchema.Decimal(value);
			else if (type == XSDDatatype.XSDdateTime)
				return new org.w3._2001_XMLSchema.DateTime(value);
			else if (type == XSDDatatype.XSDanyURI)
				return new org.w3._2001_XMLSchema.AnyURI(value);
		}
		return null;
	}
}
