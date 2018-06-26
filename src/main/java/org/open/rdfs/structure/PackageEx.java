package org.open.rdfs.structure;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.open.Util;
import org.open.rdfs.RDFSFile;
import org.open.rdfs.RDFSUtil;
import org.open.structure.Field;
import org.open.structure.Package;
import org.open.structure.Type;

/**
 * The binding class to bind a model and its corresponding package
 * 
 * @author wxlfrank
 *
 */
public class PackageEx extends Binding implements Configurable {

	private Map<String, TypeEx> annoymous = new HashMap<String, TypeEx>();

	public PackageEx(RDFS2Structure rdfs2java, String namespace, Model model) {
		super(rdfs2java, model, new Package(namespace));
	}

	public ClassEx getClassEx(String id) {
		return (ClassEx) getContents().get(id);
	}

	public Collection<Binding> getClassexes() {
		return getContents().values();
	}

	/**
	 * Retrieve the domains of the resource; there maybe exists a collection of
	 * resources which are the domains of the resource
	 * 
	 * @param property
	 *            the resource
	 * @return the domains of the resource
	 */
	public Set<ClassEx> getDomains(Resource property) {
		Set<ClassEx> classes = new LinkedHashSet<ClassEx>();
		RDFSUtilWithCache cache = RDFSUtilWithCache.INSTANCE;
		if (property != null) {
			Set<Resource> domains = cache.getDomainsOfPropertyWithCache(property);
			if (!domains.isEmpty()) {
				// For each domain of the resource, translate the domain to a Class
				// return the collection of Classes
				for (Resource domain : domains) {
					classes.add(toClassEx(domain));
				}
			} else {
				// find the super property that the property extends
				Resource sub = RDFSUtil.getSubPropertyOfProperty(property);
				if (sub != null) {
					Entry<PackageEx, Resource> real = getRealResource(sub);
					real.getKey().toFieldEx(real.getValue()).forEach(field -> {
						classes.add(field.getClassEx());
					});
				}
			}
		}
		Util.addWarning("Warning: Property " + RDFSUtil.getId(property) + " has no domains.");
		return classes;
	}

	@Override
	public String getHash() {
		return getPackage().getName();
	}

	public Model getModel() {
		return (Model) getSource();
	}

	public Package getPackage() {
		return (Package) getTarget();
	}

	public RDFS2Structure getContainer() {
		return (RDFS2Structure) super.getContainer();
	}

	/**
	 * Retrieve the ranges of the resource. There may exist a collection of ranges
	 * 
	 * @param packEx
	 * @param rdfsProperty
	 * @return the ranges of the resource
	 */
	public Set<TypeEx> getRanges(Resource rdfsProperty) {
		Set<TypeEx> ranges = new LinkedHashSet<TypeEx>();
		if (rdfsProperty != null) {
			RDFSUtilWithCache cache = RDFSUtilWithCache.INSTANCE;
			Set<Resource> rdfsRanges = cache.getRangesOfPropertyWithCache(rdfsProperty);
			if (!rdfsRanges.isEmpty()) {
				for (Resource range : rdfsRanges) {
					if (range.isAnon()) {
						String id = range.getId().toString();
						TypeEx type = getType(id);
						if (type == null) {
							Set<Resource> types = RDFSUtil.getTypes(range);
							boolean isclass = RDFSUtil.isClass(types);
							if (isclass) {
								List<Resource> union = RDFSUtil.getUnionOf(range);
								if (union != null) {
									for (Resource iter : union) {
										ranges.add(toClassEx(iter.getLocalName(), iter));
									}
								}
							}
						} else
							ranges.add(type);
					} else {
						Entry<PackageEx, Resource> real = getRealResource(range);
						PackageEx realPackage = real.getKey();
						Resource realFieldType = real.getValue();
						ranges.add(realPackage.toClassEx(realFieldType.getLocalName(), realFieldType));
					}
				}
			} else {
				Resource sub = RDFSUtil.getSubPropertyOfProperty(rdfsProperty);
				if (sub != null) {
					Entry<PackageEx, Resource> real = getRealResource(sub);
					Set<FieldEx> fields = real.getKey().toFieldEx(real.getValue());
					if (!fields.isEmpty()) {
						ranges = fields.iterator().next().getTypes();
					}
				}
			}
		}
		if (ranges.isEmpty()) {
			Util.addWarning("Warning: Property " + RDFSUtil.getId(rdfsProperty) + " has no range.");
		}
		return ranges;
	}

	/**
	 * find the real resource of the {@code resource}. The real resource may be the
	 * same as the resource or exists in another model, but has the same URI as the
	 * resource.
	 * 
	 * @param resource
	 * @return
	 */
	public Map.Entry<PackageEx, Resource> getRealResource(Resource resource) {
		String real_ns = resource.getNameSpace();
		if (real_ns != null)
			real_ns = RDFSUtil.unifyNS(real_ns);
		String ns = getPackage().getName();
		// if resource is a local resource or the resource has the same namespace as the
		// model, return the resource and its model
		if (real_ns == null || ns.equals(real_ns))
			return new AbstractMap.SimpleEntry<PackageEx, Resource>(this, resource);
		// If the resource has different namespace as the model, read the model and find
		// the corresponding resource, return the model and the resource found
		PackageEx packageEx = getContainer().getPackageEx(real_ns);
		if (packageEx == null) {
			RDFSFile rdfsFile = getConfiguration().readWriteModel(real_ns, "TURTLE");
			packageEx = getContainer().toPackageEx(real_ns, rdfsFile.getModel(), false);
			resource = packageEx.getModel().getResource(resource.getURI());
		}
		return new AbstractMap.SimpleEntry<PackageEx, Resource>(packageEx, resource);
	}

	public TypeEx getType(String id) {
		return annoymous.get(id);
	}

	public ClassEx toClassEx(Resource resource) {
		Entry<PackageEx, Resource> real = getRealResource(resource);
		String local = resource.getLocalName();
		return real.getKey().toClassEx(local, real.getValue());
	}

	public ClassEx toDataTypeEx(Resource resource) {
		Entry<PackageEx, Resource> real = getRealResource(resource);
		String local = resource.getLocalName();
		return real.getKey().toDataTypeEx(local, real.getValue());
	}

	/**
	 * Translate a property {@link Resource} to a {@link FieldEx}
	 */
	public Set<FieldEx> toFieldEx(Resource resource) {
		String local = resource.getLocalName();
		Entry<PackageEx, Resource> real = getRealResource(resource);
		Set<FieldEx> results = real.getKey().toFieldEx(local, real.getValue());
		if (real.getKey() != this)
			results.addAll(toFieldEx(local, resource));
		return results;
	}

	/**
	 * Translate a {@code property} to a field of the {@code _class}
	 * 
	 * @param rdfsProperty
	 * @param clazz
	 * @return
	 */
	protected FieldEx toFieldEx(Resource rdfsProperty, ClassEx clazz) {
		String local = rdfsProperty.getLocalName();
		Entry<PackageEx, Resource> real = getRealResource(rdfsProperty);
		return real.getKey().toFieldEx(clazz, local, real.getValue());
	}

	/**
	 * Translate all the properties of the {@code domain} to fields of the
	 * {@code clazz}
	 * 
	 * @param domain
	 * @param clazz
	 */
	protected void supplementClassEx(Resource domain, ClassEx clazz) {
		for (Resource rdfsProperty : RDFSUtilWithCache.INSTANCE.getPropertiesOfDomain(domain)) {
			toFieldEx(rdfsProperty, clazz);
		}
	}

	/**
	 * get the super class of a resource.
	 * <a href="http://semanticweb.org/wiki/Rdfs_subClassOf.html">Multiple class are
	 * possible</a>
	 * 
	 * @param rdfsClass
	 * @return
	 */
	private Set<ClassEx> getSuperClass(Resource rdfsClass) {
		Set<ClassEx> parents = new LinkedHashSet<ClassEx>();
		for (Resource parent : RDFSUtilWithCache.INSTANCE.getSuperClass(rdfsClass)) {
			if (!parent.equals(rdfsClass)) {
				parents.add(toClassEx(parent));
			}
		}
		return parents;
	}

	/**
	 * Translate a {@code resource} to a class
	 * 
	 * @param className
	 * @param rdfsClass
	 * @return
	 */
	private ClassEx toClassEx(String className, Resource rdfsClass) {
		ClassEx classex = getClassEx(className);
		if (classex == null) {
			boolean isDataType = RDFSUtilWithCache.INSTANCE.getDataTypes(rdfsClass.getModel()).contains(rdfsClass);
			classex = isDataType ? new DataTypeEx(this, className, rdfsClass) : new ClassEx(this, className, rdfsClass);
			classex.setParents(getSuperClass(rdfsClass));
			supplementClassEx(rdfsClass, classex);
		}
		return classex;
	}

	private ClassEx toDataTypeEx(String className, Resource rdfsClass) {
		ClassEx classex = getClassEx(className);
		if (classex == null) {
			classex = new DataTypeEx(this, className, rdfsClass);
			classex.setParents(getSuperClass(rdfsClass));
			supplementClassEx(rdfsClass, classex);
		}
		return classex;
	}

	/**
	 * Tranform a resource into field
	 * 
	 * @param resource
	 *            the property to be transformed
	 * @param packEx
	 * @return transformed fields
	 */
	protected Set<FieldEx> toFieldEx(String local, Resource resource) {
		// find the domains of the property
		Set<FieldEx> fields = new LinkedHashSet<FieldEx>();
		Set<ClassEx> domains = getDomains(resource);
		if (domains.isEmpty())
			return fields;
		// find the ranges of the property
		Set<TypeEx> ranges = null;
		// find the type of the ranges
		// Type rangeType = getType(ranges);
		Set<Type> types = new LinkedHashSet<Type>();
		/**
		 * create a field in each of the found domains
		 */
		for (ClassEx domain : domains) {
			FieldEx fieldEx = domain.getFieldEx(local);
			if (fieldEx == null) {
				fieldEx = new FieldEx(domain, local, resource);
				// set the ranges of the field
				if (ranges == null) {
					ranges = getRanges(resource);
					// find the type of the ranges
					// Type rangeType = getType(ranges);
					types = RDFS2StructureUtil.getTypes(ranges);
				}
				fieldEx.getTypes().addAll(ranges);
				fieldEx.getField().getTypes().addAll(types);
			}
			fields.add(fieldEx);
		}
		return fields;
	}

	private RDFSUtilWithCache cache = RDFSUtilWithCache.INSTANCE;

	private FieldEx toFieldEx(ClassEx classEx, String local, Resource rdfsProperty) {
		/**
		 * create a field in each of the found domains
		 */
		FieldEx fieldEx = classEx.getFieldEx(local);
		if (fieldEx == null) {
			fieldEx = new FieldEx(classEx, local, rdfsProperty);
			Set<TypeEx> ranges = getRanges(rdfsProperty);
			fieldEx.getTypes().addAll(ranges);
			fieldEx.getField().getTypes().addAll(RDFS2StructureUtil.getTypes(ranges));
		}
		return fieldEx;
	}

	@Override
	public RDFSExConfig getConfiguration() {
		return getContainer().getConfiguration();
	}

	public void handleShape(ClassEx classEx, Resource nodeShape) {
		for (Resource propertyShape : cache.getShaclProperties(nodeShape)) {
			Resource path = RDFSUtil.getPath(propertyShape);
			FieldEx fieldEx = toFieldEx(path, classEx);
			handleShape(fieldEx, propertyShape);
		}
	}

	private void handleShape(FieldEx fieldEx, Resource propertyShape) {
		Field field = fieldEx.getField();
		handleMultiplicity(field, propertyShape);
		Set<ClassEx> classexes = handleConstraints(propertyShape);
		fieldEx.getTypes().addAll(classexes);
		fieldEx.getField().getTypes().addAll(RDFS2StructureUtil.getTypes(classexes));
	}

	public Set<ClassEx> handleConstraints(Resource shape) {
		Set<ClassEx> results = new LinkedHashSet<ClassEx>();
		// A shape has at most one value for sh:datatype.
		Resource dataType = RDFSUtil.getDatatypeConstraint(shape);
		if (dataType != null) {
			results.add(toClassEx(dataType));
			return results;
		}
		/*
		 * class constraint sh:class specifies that each value node is a SHACL instance
		 * of a given type multiple values for sh:class are interpreted as a
		 * conjunction, i.e. the values need to be SHACL instances of all of them.
		 */
		for (Resource iter : RDFSUtil.getClassConstraint(shape)) {
			results.add(toClassEx(iter));
		}

		RDFSUtilWithCache cache = RDFSUtilWithCache.INSTANCE;
		for (Resource iter : RDFSUtil.getShapeConstraint(shape)) {
			Set<ClassEx> classes = cache.getShapeTypes(iter);
			if (classes == null) {
				classes = handleConstraints(iter);
				cache.setShapeTypes(iter, classes);
			}
			results.addAll(classes);
		}
		for (Resource iter : RDFSUtil.getOrConstraint(shape)) {
			results.addAll(handleConstraints(iter));
		}
		return results;
	}

	private void handleMultiplicity(Field field, Resource propertyShape) {
		int preMin = field.getMin(), preMax = field.getMax();
		/*
		 * The maximum cardinality. Node shapes cannot have any value for sh:maxCount. A
		 * property shape has at most one value for sh:maxCount. The values of
		 * sh:maxCount in a property shape are literals with datatype xsd:integer.
		 */
		Integer max = RDFSUtil.getMaxCount(propertyShape);
		if (max != null) {
			if (max > preMin && max < preMax)
				field.setMax(max);
		}
		/*
		 * The minimum cardinality. Node shapes cannot have any value for sh:minCount. A
		 * property shape has at most one value for sh:minCount. The values of
		 * sh:minCount in a property shape are literals with datatype xsd:integer.
		 */
		Integer min = RDFSUtil.getMinCount(propertyShape);
		if (min != null) {
			if (min > preMin && min < preMax)
				field.setMin(min);
		}
	}

	public void handleShape() {
		RDFSUtilWithCache cache = RDFSUtilWithCache.INSTANCE;
		for (Resource shape : cache.getShapesOfModel(getModel())) {
			for (Resource clazzResource : cache.getClassesOfShape(shape)) {
				if (cache.isShapeHandled(clazzResource))
					continue;
				ClassEx clazz = toClassEx(clazzResource);
				handleShapeOfClass(clazz);
			}
		}
	}

	private void handleShapeOfClass(ClassEx classEx) {
		RDFSUtilWithCache cache = RDFSUtilWithCache.INSTANCE;

		Resource source = classEx.getSource();
		Set<Resource> shapes = cache.getShapesOfClass(source);
		if (shapes.isEmpty())
			return;
		cache.setShapeHandled(source);
		for (ClassEx parent : classEx.getParents()) {
			if (cache.isShapeHandled(parent.getSource()))
				parent.getContainer().handleShapeOfClass(parent);
		}
		for (Resource shape : cache.getShapesOfClass(classEx.getSource())) {
			handleShape(classEx, shape);
		}
	}

}
