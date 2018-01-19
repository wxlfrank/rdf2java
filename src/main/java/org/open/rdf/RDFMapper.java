package org.open.rdf;

import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.open.rdf.annotation.RDF;
import org.open.rdfs.RDFSUtils;
import org.open.rdfs.Utils;

public class RDFMapper {

	/**
	 * The hash table that maps a java field to a RDF property
	 */
	private Map<Field, Property> field2rdfProperty = new HashMap<Field, Property>();
	/**
	 * The hash table that maps a resource to a java object, which is stored by the
	 * object's class
	 */
	private final Map<Class<?>, Map<Resource, Object>> java_class2rdf_resource2java_object = new HashMap<Class<?>, Map<Resource, Object>>();

	private RDFMapperConfig config = new RDFMapperConfig();

	// the hash table which map a java runtime class to a RDF resource which is used
	// to specify the class of a RDF instance
	private Map<Class<?>, Resource> javaclass2rdftype = new HashMap<Class<?>, Resource>();

	/**
	 * The hash table that maps the RDF resource which is a RDF:Class to its
	 * corresponding java class.
	 */
	private final Map<Resource, Class<?>> rdf_class2java_class = new HashMap<Resource, Class<?>>();

	/**
	 * The java objects that are translated from RDF resources
	 */
	private final Set<Object> read_objects = new HashSet<Object>();

	/**
	 * The java objects that are translated from RDF resources and are the root of
	 * the read_objects
	 */
	private final Set<Object> read_topmost = new HashSet<Object>();

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
	public Resource findRDFClass(Model rdf_model, Class<?> java_runtime_class) {
		Resource result = javaclass2rdftype.get(java_runtime_class);
		if (result == null) {
			RDF annotation = java_runtime_class.getAnnotation(RDF.class);
			if (null == annotation)
				return null;
			result = rdf_model.createResource(annotation.namespace() + annotation.local());
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
		while (stmts.hasNext()) {
			Statement stmt = stmts.next();
			Resource rdf_subject = stmt.getSubject();
			Map.Entry<Object, Boolean> entry = toJavaObject(rdf_subject);
			if (entry != null) {
				boolean new_created = entry.getValue();
				Object java_subject = entry.getKey();
				if (new_created) {
					read_topmost.add(java_subject);
				}
				entry = toJava(stmt.getObject());
				if (entry != null) {
					Object java_object = entry.getKey();
					new_created = entry.getValue();
					if (!java_object.getClass().isPrimitive()) {
						if (new_created) {
							read_objects.add(java_object);
						} else {
							if (read_topmost.contains(java_object))
								read_topmost.remove(java_object);
						}
					}

					Object accessor = util.getJavaFieldSetter(java_subject.getClass())
							.get(stmt.getPredicate().getURI());
					util.setProperty(accessor, java_subject, java_object);
				}
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
	public Resource toRDFResource(Model rdf_model, Object java_object) {
		// if obj is a collection, transform obj to a list of resources
		if (java_object instanceof Collection<?>) {
			RDFList list = rdf_model.createList();
			for (Object iter : (Collection<?>) java_object) {
				list.add(toRDFResource(rdf_model, iter));
			}
			return list;
		}
		// if obj is a single object, transform it to a single resource
		Resource resource = rdf_model.createResource();
		Resource type = findRDFClass(rdf_model, java_object.getClass());
		if (type != null)
			resource.addProperty(org.apache.jena.vocabulary.RDF.type, type);
		toRDFStatement(rdf_model, resource, java_object);
		return resource;
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
			if (java_value == null)
				continue;
			Property rdf_property = findRDFProperty(model, field);
			if (rdf_property == null)
				continue;
			Class<?> cls = java_value.getClass();
			if (cls.isPrimitive()) {
				rdf_subject.addLiteral(rdf_property, java_value);
			} else
				rdf_subject.addProperty(rdf_property, toRDFResource(model, java_value));
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
			RDF annotation = field.getAnnotation(RDF.class);
			if (null == annotation)
				return null;
			result = model.createProperty(annotation.namespace(), annotation.local());
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
	private Map.Entry<Object, Boolean> getJavaObject(Resource rdf_resource, Class<?> java_class) {
		Object result = Utils.get(java_class2rdf_resource2java_object, java_class, rdf_resource);
		boolean created = false;
		if (result == null) {
			try {
				result = java_class.newInstance();
				created = true;
			} catch (InstantiationException | IllegalAccessException e) {
				return null;
			}
			Utils.put(java_class2rdf_resource2java_object, java_class, rdf_resource, result);
		}
		return new AbstractMap.SimpleEntry<Object, Boolean>(result, created);
	}

	/**
	 * Translate an RDF node {@code rdf_node} to a java value, whether a java object
	 * or a java primitive, depending on the type of the RDF node
	 * 
	 * @param rdf_node
	 *            the RDF node
	 * @return the java value
	 */
	private Map.Entry<Object, Boolean> toJava(RDFNode rdf_node) {
		if (rdf_node.isResource()) {
			return toJavaObject(rdf_node.asResource());
		}
		return null;
	}

	/**
	 * Translate a RDF resource to a java object
	 * 
	 * @param rdf_resource
	 *            the RDF resource
	 * @return the translated java object and whether the object is newly created or
	 *         not
	 */
	private Map.Entry<Object, Boolean> toJavaObject(Resource rdf_resource) {
		Resource type = RDFSUtils.getType(rdf_resource);
		if (type == null)
			return null;
		Class<?> cls = getClassForURI(type);
		if (cls == null)
			return null;
		return getJavaObject(rdf_resource, cls);
	}
}
