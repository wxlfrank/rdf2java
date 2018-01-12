package org.epos.rdf;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.epos.rdf.annotation.RDF;

public class RDFMapper {

	private MapperConfig config = new MapperConfig();
	private Map<Class<?>, Resource> HASH_CLASS = new HashMap<Class<?>, Resource>();

	private Map<Field, Property> HASH_PROPERTY = new HashMap<Field, Property>();

	Resource currentSource = null;

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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return null;
	}

	public void handleField(Model model, Resource currentSource, Object obj) {
		java.lang.Class<?> clz = obj.getClass();
		for (Field field : clz.getDeclaredFields()) {
			Object value = getFieldValue(clz, field, obj);
			if (value == null)
				continue;
			Property property = findProperty(model, field);
			if (property == null)
				continue;
			Class<?> cls = field.getType();
			if (cls.isPrimitive()) {
				currentSource.addLiteral(property, value);
			} else
				currentSource.addProperty(property, getObject(model, value));
		}
	}
	public Model toRDF(Object obj) {
		Model model = ModelFactory.createDefaultModel();
		toRDFResource(model, obj);
		return model;
	}

	public void toRDFResource(Model model, Object obj) {
		if (obj instanceof Collection<?>) {
			Collection<?> collection = (Collection<?>) obj;
			collection.forEach(iter -> toRDFResource(model, iter));
			return;
		}
		Resource result = model.createResource();
		Resource type = findType(model, obj.getClass());
		if (type != null)
			result.addProperty(org.apache.jena.vocabulary.RDF.type, type);
		handleField(model, result, obj);
	}

	private Property findProperty(Model model, Field field) {
		Property result = HASH_PROPERTY.get(field);
		if (result == null) {
			RDF annotation = field.getAnnotation(RDF.class);
			if (null == annotation)
				return null;
			result = model.createProperty(annotation.namespace(), annotation.local());
		}
		return result;
	}

	private Resource findType(Model model, Class<?> cls) {
		Resource result = HASH_CLASS.get(cls);
		if (result == null) {
			RDF annotation = cls.getAnnotation(RDF.class);
			if (null == annotation)
				return null;
			result = model.createResource(annotation.namespace() + annotation.local());
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
