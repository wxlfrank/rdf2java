package org.open.rdfs;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

public class RDFSUtil {
	public static String getId(Resource resource) {
		return resource.isURIResource() ? resource.getURI() : resource.getId().toString();
	}

	public static List<Resource> getList(Resource resource) {
		Statement first = resource.getProperty(RDF.first);
		if (first != null) {
			List<Resource> result = new ArrayList<Resource>();
			result.add(first.getObject().asResource());
			Statement rest = resource.getProperty(RDF.rest);
			if (rest != null) {
				List<Resource> others = getList(rest.getObject().asResource());
				if (others != null)
					result.addAll(others);
			}
			return result;
		}
		return null;
	}

	public static List<Resource> getUnionOf(Resource resource) {
		Statement stm = resource.getProperty(OWL.unionOf);
		if (stm != null) {
			return getList(stm.getObject().asResource());
		}
		return null;
	}

	public static Resource getTargetClassOfNodeShape(Resource shape) {
		return getPropertyAsResource(shape, SHACL.targetClass);
	}

	public static Resource getType(RDFNode resource) {
		return getPropertyAsResource(resource, RDF.type);
	}

	public static Set<Resource> getTypes(Resource resource) {
		return getProperties(resource, RDF.type);
	}

	public static Set<Resource> getProperties(Resource resource, Property property) {
		Set<Resource> results = new LinkedHashSet<Resource>();
		resource.getModel().listStatements(resource, property, (RDFNode) null)
				.forEachRemaining(stm -> results.add(stm.getObject().asResource()));
		return results;
	}

	public static RDFNode getProperty(RDFNode resource, Property property) {
		if (!(resource instanceof Resource))
			return null;
		Statement stm = ((Resource)resource).getProperty(property);
		if (stm == null)
			return null;
		return stm.getObject();
	}

	public static Literal getPropertyAsLiteral(Resource resource, Property property) {
		RDFNode value = getProperty(resource, property);
		return value != null ? value.asLiteral() : null;
	}

	public static Resource getPropertyAsResource(RDFNode resource, Property property) {
		RDFNode value = getProperty(resource, property);
		return value != null ? value.asResource() : null;
	}

	public static Set<Resource> getClassConstraint(Resource property) {
		return getProperties(property, SHACL.clazz);
	}
	public static Set<Resource> getShapeConstraint(Resource property) {
		return getProperties(property, SHACL.node);
	}

	public static Resource getPath(Resource property) {
		return getPropertyAsResource(property, SHACL.path);
	}

	public static Integer getMaxCount(Resource property) {
		Literal maxCount = getPropertyAsLiteral(property, SHACL.maxCount);
		if (maxCount != null) {
			try {
				return Integer.parseInt(maxCount.getString());
			} catch (NumberFormatException e) {
			}
		}
		return Integer.MAX_VALUE;
	}

	public static Integer getMinCount(Resource property) {
		Literal minCount = getPropertyAsLiteral(property, SHACL.minCount);
		if (minCount != null) {
			try {
				return Integer.parseInt(minCount.getString());
			} catch (NumberFormatException e) {
			}
		}
		return 0;
	}

	public static Resource getShaclNodeKind(Resource property) {
		return getPropertyAsResource(property, SHACL.nodeKind);
	}

	/**
	 * retrieve all the super property that the property extends
	 * 
	 * @param property
	 * @return
	 */
	public static Resource getSubPropertyOfProperty(Resource property) {
		return getPropertyAsResource(property, RDFS.subPropertyOf);
	}

	public static boolean isClass(Set<Resource> types) {
		for (Resource type : types) {
			if (type.equals(RDFS.Class) || type.equals(OWL.Class))
				return true;
		}
		return false;
	}

	public static Resource getDatatypeConstraint(Resource propertyShape) {
		return getPropertyAsResource(propertyShape, SHACL.datatype);
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

	public static List<Resource> getOrConstraint(Resource property) {
		List<Resource> results = new ArrayList<Resource>();
		Resource ors = getPropertyAsResource(property, SHACL.or);
		if(ors == null) return results;
		return getList(ors);
	}
}
