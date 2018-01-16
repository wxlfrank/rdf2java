package org.epos.rdf;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.epos.rdf.annotation.RDF;
import org.epos.tranform.TransformUtils;

public class RDFWriter {

	private MapperConfig config = new MapperConfig();
	// the hash table which map a java runtime class to a RDF resource which is used
	// to specify the class of a RDF instance
	private Map<Class<?>, Resource> class2rdfClass = new HashMap<Class<?>, Resource>();

	private Map<Field, Property> field2rdfProperty = new HashMap<Field, Property>();


	private Map<Class<?>, Map<Field, Object>> class2fieldAccessor = new HashMap<Class<?>, Map<Field, Object>>();

	public Object getFieldValue(Class<?> clz, Field field, Object obj) {
		String name = field.getName();
		if (field.isAccessible()) {
			try {
				return field.get(obj);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		} else {
			try {
				Method method = clz.getMethod("get" + name.substring(0, 1).toUpperCase() + name.substring(1));
				return method.invoke(obj);
			} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}

		}
		return null;
	}

	public void handleField(Model model, Resource subject, Object obj) {
		java.lang.Class<?> clz = obj.getClass();
		Map<Field, Object> fieldAccessor = getJavaFieldAccessor(clz);
		for (Entry<Field, Object> accessor : fieldAccessor.entrySet()) {
			Field field = accessor.getKey();
			Object value = getJavaFieldValue(clz, field, accessor.getValue(), obj);
			if (value == null)
				continue;
			Property property = findRDFProperty(model, field);
			if (property == null)
				continue;
			Class<?> cls = value.getClass();
			if (cls.isPrimitive()) {
				subject.addLiteral(property, value);
			} else
				subject.addProperty(property, toRDFResource(model, value));
		}
	}

	/**
	 * create field accessor for each field of the {@code java_class}
	 * 
	 * @param java_class
	 * @return the created field accessors
	 */
	protected Map<Field, Object> createFieldAccessor(Class<?> java_class) {
		Map<Field, Object> fieldAccessor = new HashMap<Field, Object>();
		for (Field field : java_class.getDeclaredFields()) {
			if (field.isAccessible()) {
				fieldAccessor.put(field, field);
			} else {
				try {
					fieldAccessor.put(field, java_class.getMethod("get" + TransformUtils.getName(field.getName())));
				} catch (NoSuchMethodException | SecurityException | IllegalArgumentException e) {
					e.printStackTrace();
				}
			}
		}
		return fieldAccessor;
	}

	/**
	 * get the filed accessors for the java class
	 * 
	 * @param java_class
	 * @return
	 */
	public Map<Field, Object> getJavaFieldAccessor(Class<?> java_class) {
		Map<Field, Object> fieldAccessor = class2fieldAccessor.get(java_class);
		if (fieldAccessor == null) {
			fieldAccessor = createFieldAccessor(java_class);
			class2fieldAccessor.put(java_class, fieldAccessor);
		}
		return fieldAccessor;
	}

	/**
	 * Get the value of the {@code field} for the {@code java_object} which is of
	 * the type {@code java_class} using the field {@code accessor}
	 * 
	 * @param java_class
	 *            a java class
	 * @param field
	 *            a java field
	 * @param accessor
	 *            the accessor of the the field, i.e., field itself or the method
	 * @param java_object
	 *            the java object
	 * @return the value
	 */
	private Object getJavaFieldValue(Class<?> java_class, Field field, Object accessor, Object java_object) {
		if (accessor == field) {
			try {
				return field.get(java_object);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		} else {
			try {
				return ((Method) accessor).invoke(java_object);
			} catch (SecurityException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}

		}
		return null;
	}

	public Model write(Object obj) {
		Model model = ModelFactory.createDefaultModel();
		// transform obj to a resource in model
		toRDFResource(model, obj);
		return model;
	}

	/**
	 * Transform an java object {@code obj} to a RDF resource in a model
	 * {@code model}
	 * 
	 * @param model
	 *            the RDF model to be created
	 * @param java_object
	 *            the java object to be transformed
	 * @return the RDF resource created
	 */
	public Resource toRDFResource(Model model, Object java_object) {
		// if obj is a collection, transform obj to a list of resources
		if (java_object instanceof Collection<?>) {
			RDFList list = model.createList();
			for (Object iter : (Collection<?>) java_object) {
				list.add(toRDFResource(model, iter));
			}
			return list;
		}
		// if obj is a single object, transform it to a single resource
		Resource resource = model.createResource();
		Resource type = findRDFClass(model, java_object.getClass());
		if (type != null)
			resource.addProperty(org.apache.jena.vocabulary.RDF.type, type);
		handleField(model, resource, java_object);
		return resource;
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
	 * Find the RDF class for a java runtime class {@code java_runtime_class}
	 * 
	 * @param rdf_model
	 *            the RDF model
	 * @param java_runtime_class
	 *            the java runtime class
	 * @return the RDF class
	 */
	public Resource findRDFClass(Model rdf_model, Class<?> java_runtime_class) {
		Resource result = class2rdfClass.get(java_runtime_class);
		if (result == null) {
			RDF annotation = java_runtime_class.getAnnotation(RDF.class);
			if (null == annotation)
				return null;
			result = rdf_model.createResource(annotation.namespace() + annotation.local());
		}
		return result;
	}

	private RDFNode getObject(Model model, Object value) {
		Class<?> clz = value.getClass();
		if (clz == int.class)
			return model.createTypedLiteral(value);
		return null;
	}
}
