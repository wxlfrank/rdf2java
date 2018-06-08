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
import java.util.function.Predicate;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.open.Util;
import org.open.rdf.annotation.RDF;
import org.open.rdf.annotation.RDFLiteral;
import org.open.rdfs.RDFSUtil;

public class RDFMapper {

	/**
	 * The hash table that maps a java field to a RDF property
	 */
	private Map<Field, Property> field2rdfProperty = new HashMap<Field, Property>();
//	/**
//	 * The hash table that maps a resource to a java object, which is stored by the
//	 * object's class
//	 */
//	private final Map<Class<?>, Map<RDFNode, Object>> java_class2rdf_resource2java_object = new HashMap<Class<?>, Map<RDFNode, Object>>();

	private RDFMapperConfig config = new RDFMapperConfig();

	// the hash table which map a java runtime class to a RDF resource which is used
	// to specify the class of a RDF instance
	private Map<Class<?>, Object> javaclass2rdftype = new HashMap<Class<?>, Object>();

	/**
	 * The hash table that maps the RDF resource which is a RDF:Class to its
	 * corresponding java class.
	 */
	private final Map<String, Class<?>> rdfClass2javaClass = new HashMap<String, Class<?>>();

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
	private JavaReflectUtils javaUtil = new JavaReflectUtils();

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
			RDF[] annotations = java_runtime_class.getAnnotationsByType(RDF.class);
			if (annotations.length > 0) {
				RDF annotation = annotations[0];
				result = rdf_model.createResource(annotation.namespace() + annotation.local());
			} else {
				RDFLiteral literal = java_runtime_class.getAnnotation(RDFLiteral.class);
				if (literal == null)
					System.out.println(java_runtime_class.getName());
				result = literal.namespace() + literal.local();
			}
			if (null == result)
				return null;
			javaclass2rdftype.put(java_runtime_class, result);
		}
		return result;
	}

	protected Class<?> findCandiate(Class<?> cls, List<Statement> statements) {
		RDF[] annotations = cls.getAnnotationsByType(RDF.class);
		Class<?> candidate = cls;
		if (annotations.length > 1) {
			Class<?>[] candidates = new Class<?>[annotations.length];
			candidates[0] = cls;
			for (int i = 1; i < candidates.length; i++) {
				RDF annotation = annotations[i];
				candidates[i] = getClassForURI(annotation.namespace(), annotation.local());
			}

			int minError = -1;
			for (Class<?> iter : candidates) {
				int errorCount = 0;
				for (Statement statement : statements) {
					Resource resource = statement.getPredicate();
					String uri = RDFSUtil.unifyNS(resource.getNameSpace()) + resource.getLocalName();
					Object accessor = javaUtil.getJavaFieldSetter(iter).get(uri);
					if (accessor == null) {
						++errorCount;
					}
				}
				if (minError == -1 || minError > errorCount) {
					minError = errorCount;
					candidate = iter;
				}
			}
		}
		return candidate;
	}

	final static private Predicate<Statement> filter = new Predicate<Statement>() {

		@Override
		public boolean test(Statement t) {
			Property predicate = t.getPredicate();
			return predicate.equals(org.apache.jena.vocabulary.RDF.type)
					|| predicate.equals(org.apache.jena.vocabulary.RDFS.label);
		}
	};

	protected Object rdfResourcetoJavaObject(Resource rdfSubject) {
		Resource rdfSubjectType = RDFSUtil.getType(rdfSubject);
		if (rdfSubjectType == null) {
			Util.addWarning("Can not find the type of the RDF subject " + RDFSUtil.getId(rdfSubject));
		}
		Class<?> javaSubjectClass = getJavaClassOfRDFResource(rdfSubjectType);
		if (javaSubjectClass == DEFAULT_CLASS) {
			// rdfResource2javaObject.put(rdfSubject, DEFAULT);
			return DEFAULT;
		}
		Model model = rdfSubject.getModel();
		List<Statement> rdfSubjectStmts = model.listStatements(rdfSubject, null, (RDFNode) null).toList();
		rdfSubjectStmts.removeIf(filter);
		javaSubjectClass = findCandiate(javaSubjectClass, rdfSubjectStmts);

		// create java object for the rdf subject
		Object javaSubject = createJavaObjectForRDFResource(rdfSubject, javaSubjectClass);
		// rdfResource2javaObject.put(rdfSubject, javaSubject);
		if (javaSubject == DEFAULT)
			return DEFAULT;
		read_topmost.add(javaSubject);

		for (Statement rdfSubjectStmt : rdfSubjectStmts) {
			// translate RDF predicate
			Resource resource = rdfSubjectStmt.getPredicate();
			String iri = RDFSUtil.unifyNS(resource.getNameSpace()) + resource.getLocalName();
			Object javaAccessor = javaUtil.getJavaFieldSetter(javaSubjectClass).get(iri);
			if (javaAccessor == null) {
				String warning = model.getNsURIPrefix(resource.getNameSpace()) + ":" + resource.getLocalName()
						+ " is not defined in epos-dcat-ap_shapes.ttl";
				warning += ". It should defined in " + model.getNsURIPrefix(rdfSubjectType.getNameSpace()) + ":"
						+ rdfSubjectType.getLocalName() + " or its superclass.";
				if (Util.addWarning(warning)) {
					System.out.println(rdfSubjectStmt);
				}
				continue;
			}
			// translate RDF object
			RDFNode rdfObject = rdfSubjectStmt.getObject();
			Object javaObject = rdfResource2javaObject.get(rdfObject);
//			if (javaObject != null) {
//				System.out.println(rdfSubjectStmt.toString());
//				System.out.println(rdfObject.toString());
//				System.out.println(javaObject);
//			}
			boolean newCreated = javaObject == null;
			if (newCreated) {
				javaObject = toJavaObject(rdfObject);
				if (!rdfObject.isLiteral())
					rdfResource2javaObject.put(rdfObject, javaObject);
			}
			if (javaObject != DEFAULT) {
				if (newCreated) {
					read_objects.add(javaObject);
				} else {
					if (read_topmost.contains(javaObject)) {
						read_topmost.remove(javaObject);
						read_objects.add(javaObject);
					}
				}
				javaUtil.setProperty(javaAccessor, javaSubject, javaObject);
			}
		}
		return javaSubject;
	}

	/**
	 * Translate a RDF model to java objects
	 * 
	 * @param model
	 *            the RDF model
	 * @return the topmost java objects
	 */
	public Object read(Model model) {
		for (Resource rdfSubject : model.listSubjects().toList()) {
			rdfResourcetoJavaObjectEx(rdfSubject);
		}
		if (read_topmost.isEmpty())
			return read_objects.isEmpty() ? null : read_objects;
		return read_topmost.size() == 1 ? read_topmost.iterator().next() : read_topmost;

	}

	private Object rdfResourcetoJavaObjectEx(Resource rdfSubject) {
		Object javaSubject = rdfResource2javaObject.get(rdfSubject);
		// If rdf_subject has been transformed to java object.
		if (javaSubject == null) {
			javaSubject = rdfResourcetoJavaObject(rdfSubject);
			rdfResource2javaObject.put(rdfSubject, javaSubject);
		}
		return javaSubject;
	}

	private Object createJavaObjectForRDFResource(Resource rdfSubject, Class<?> javaClass) {
		Object result = null;
		try {
			result = javaClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			Util.addWarning(
					"Can not create an java instance of " + javaClass.getName() + " for " + RDFSUtil.getId(rdfSubject));
			result = DEFAULT;
		}
		return result;
	}

	private Class<?> getJavaClassOfRDFResource(Resource resource) {
		// Resource rdfResourceType = RDFSUtil.getType(resource);
		// if (rdfResourceType == null) {
		// Util.addWarning("Can not find the type of the RDF subject " +
		// RDFSUtil.getId(resource));
		// }
		return resource != null ? getClassForURI(resource.getNameSpace(), resource.getLocalName()) : DEFAULT_CLASS;
		// return getClassForURI(resource.getNameSpace(), resource.getLocalName());
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
				if (iter == null)
					System.out.println("");
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
		Map<Field, Object> fieldAccessor = javaUtil.getJavaFieldGetter(java_class);
		for (Entry<Field, Object> field_accessor : fieldAccessor.entrySet()) {
			Field field = field_accessor.getKey();
			Object java_accessor = field_accessor.getValue();
			Object java_value = javaUtil.getJavaFieldValue(java_class, java_accessor, java_object);
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
			result = model.createProperty(annotation.namespace(), annotation.local());
			field2rdfProperty.put(field, result);
		}
		return result;
	}

	private static final Class<?> DEFAULT_CLASS = Object.class;

	/**
	 * Get the java class for the RDF class {@code}
	 * 
	 * @param rdf_class
	 *            the RDF class
	 * @return the corresponding java class
	 */
	private Class<?> getClassForURI(String namespace, String local) {
		String uri = namespace + local;
		Class<?> result = rdfClass2javaClass.get(uri);
		if (result == null) {
			try {
				result = Class.forName(config.getPackageName(namespace) + "." + local);
				rdfClass2javaClass.put(uri, result);
			} catch (ClassNotFoundException e) {
				rdfClass2javaClass.put(uri, DEFAULT_CLASS);
				Util.addWarning("Can not find a java class for the resource " + uri);
				return DEFAULT_CLASS;
			}
		}
		return result;
	}

//	/**
//	 * Get the java object which is instance of a java class {@code java_class}
//	 * corresponding to a RDF resource
//	 * 
//	 * @param rdfResource
//	 *            the RDF resource
//	 * @param javaClass
//	 *            the java class
//	 * @return the java object
//	 */
//	private Object getJavaObject(RDFNode rdfResource, Class<?> javaClass) {
//		Object javaObject = Util.getFromCache(java_class2rdf_resource2java_object, javaClass, rdfResource);
//		if (javaObject == null) {
//			try {
//				javaObject = javaClass.newInstance();
//			} catch (InstantiationException | IllegalAccessException e) {
//				javaObject = DEFAULT;
//			}
//			Util.putIntoCache(java_class2rdf_resource2java_object, javaClass, rdfResource, javaObject);
//		}
//		return javaObject;
//	}

	private static final Object DEFAULT = new Object();
	Map<RDFNode, Object> rdfResource2javaObject = new HashMap<RDFNode, Object>();

	/**
	 * Translate a RDF resource to a java object
	 * 
	 * @param rdfNode
	 *            the RDF resource
	 * @return the translated java object and whether the object is newly created or
	 *         not
	 */
	private Object toJavaObject(RDFNode rdfNode) {
		Object javaObject = null;
		if (rdfNode instanceof Resource) {
			Resource rdfResource = (Resource) rdfNode;
			List<Resource> rdfResources = RDFSUtil.getList(rdfResource);
			if (rdfResources == null) {
				return rdfResourcetoJavaObjectEx(rdfResource);
			} else {
				List<Object> javaObjects = new ArrayList<Object>();
				for (Resource iter : rdfResources) {
					javaObject = rdfResourcetoJavaObjectEx(iter);
					if (javaObject == DEFAULT)
						continue;
					if (read_topmost.contains(javaObject)) {
						read_topmost.remove(javaObject);
						read_objects.add(javaObject);
					}
					javaObjects.add(javaObject);
				}
				// rdfResource2javaObject.put(rdfNode, javaObjects);
				return javaObjects;
			}
		} else if (rdfNode instanceof Literal) {
			Literal literal = (Literal) rdfNode;
			RDFDatatype rdfDataType = literal.getDatatype();
			Object value = literal.getValue();
			if (rdfDataType == XSDDatatype.XSDstring) {
				return new org.w3._2001_XMLSchema.String(value);
			} else if (rdfDataType == XSDDatatype.XSDdate)
				return new org.w3._2001_XMLSchema.Date(value);
			else if (rdfDataType == XSDDatatype.XSDboolean)
				return new org.w3._2001_XMLSchema.Boolean(value);
			else if (rdfDataType == XSDDatatype.XSDint)
				return new org.w3._2001_XMLSchema.Integer(value);
			else if (rdfDataType == XSDDatatype.XSDfloat)
				return new org.w3._2001_XMLSchema.Float(value);
			else if (rdfDataType == XSDDatatype.XSDdouble)
				return new org.w3._2001_XMLSchema.Decimal(value);
			else if (rdfDataType == XSDDatatype.XSDdateTime)
				return new org.w3._2001_XMLSchema.DateTime(value);
			else if (rdfDataType == XSDDatatype.XSDanyURI)
				return new org.w3._2001_XMLSchema.AnyURI(value);
		}
		return DEFAULT;
	}

}
