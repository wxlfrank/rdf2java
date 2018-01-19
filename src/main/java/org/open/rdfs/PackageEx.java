package org.open.rdfs;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

/**
 * The binding class to bind a model and its corresponding package
 * 
 * @author wxlfrank
 *
 */
public class PackageEx extends Binding {

	private Map<String, TypeEx> annoymous = new HashMap<String, TypeEx>();

	private Map<String, Interface> types2interface = new HashMap<String, Interface>();

	public PackageEx(RDFSEx rdfs2java, Model model, Package result) {
		super(rdfs2java, model, result);
	}

	public ClassEx createClass(String local, Resource resource) {
		Class cls = new Class(getPackage(), local);
		ClassEx result = new ClassEx(this, resource, cls);
		return result;
	}

	public ClassEx getClassEx(String id) {
		return (ClassEx) getChildren().get(id);
	}

	public Collection<Binding> getClassexes() {
		return getChildren().values();
	}

	/**
	 * Retrieve the domains of the resource; there maybe exists a collection of
	 * resources which are the domains of the resource
	 * 
	 * @param resource
	 *            the resource
	 * @return the domains of the resource
	 */
	public Set<ClassEx> getDomains(Resource resource) {
		if (resource != null) {
			Set<ClassEx> results = new HashSet<ClassEx>();
			Set<Resource> domains = RDFSUtils.getDomains(resource);
			if (!domains.isEmpty()) {
				// For each domain of the resource, translate the domain to a Class
				// return the collection of Classes
				for (Resource domain : domains) {
					results.add(toClassEx(domain));
				}
			} else {
				// find the super property that the property extends
				Resource sub = RDFSUtils.getSubProperty(resource);
				if (sub != null) {
					Entry<PackageEx, Resource> real = getRealResource(sub);
					real.getKey().toFieldEx(real.getValue()).forEach(field -> {
						results.add(field.getClassEx());
					});
				}
			}
			return results;
		}
		System.out.println("Warning: Property " + RDFSUtils.getId(resource) + " has no domains.");
		return null;
	}

	@Override
	public String getHashString() {
		return getPackage().getName();
	}

	public Model getModel() {
		return (Model) getSource();
	}

	public Package getPackage() {
		return (Package) getTarget();
	}

	public RDFSEx getParent() {
		return (RDFSEx) super.getParent();
	}

	/**
	 * Retrieve the ranges of the resource. There may exist a collection of ranges
	 * 
	 * @param packEx
	 * @param resource
	 * @return the ranges of the resource
	 */
	public Set<TypeEx> getRanges(Resource resource) {
		Set<TypeEx> result = new HashSet<TypeEx>();
		if (resource != null) {
			Set<Resource> ranges = RDFSUtils.getRanges(resource);
			if (!ranges.isEmpty()) {
				for (Resource range : ranges) {
					if (range.isAnon()) {
						String id = range.getId().toString();
						TypeEx type = getType(id);
						if (type == null) {
							Set<Resource> types = RDFSUtils.getTypes(range);
							boolean isclass = RDFSUtils.isClass(types);
							if (isclass) {
								List<Resource> union = RDFSUtils.getUnionOf(range);
								if (union != null) {
									for (Resource iter : union) {
										System.out.println(RDFSUtils.getId(iter));
										result.add(toClassEx(iter.getLocalName(), iter));
									}
								}
							}
						} else
							result.add(type);
					} else {
						Entry<PackageEx, Resource> real = getRealResource(range);
						PackageEx realPackage = real.getKey();
						Resource realSuperClass = real.getValue();
						result.add(realPackage.toClassEx(realSuperClass.getLocalName(), realSuperClass));
					}
				}
				return result;
			} else {
				Resource sub = RDFSUtils.getSubProperty(resource);
				if (sub != null) {
					Entry<PackageEx, Resource> real = getRealResource(sub);
					Set<FieldEx> fields = real.getKey().toFieldEx(real.getValue());
					if (!fields.isEmpty()) {
						return fields.iterator().next().getTypes();
					}
				}
			}
		}
		System.out.println(
				"Warning: the property " + RDFSUtils.getId(resource) + " has no range. Use String as default.");
		result.add(Constants.XMLSchema_Package.getClassEx("string"));
		return result;
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
			real_ns = getParent().getConfig().unifyNS(real_ns);
		String ns = getPackage().getName();
		// if resource is a local resource or the resource has the same namespace as the
		// model, return the resource and its model
		if (real_ns == null || ns.equals(real_ns))
			return new AbstractMap.SimpleEntry<PackageEx, Resource>(this, resource);
		// If the resource has different namespace as the model, read the model and find
		// the corresponding resource, return the model and the resource found
		PackageEx packageEx = getParent().getPackageEx(real_ns);
		if (packageEx == null) {
			Entry<String, Model> model = getParent().getConfig().readWriteModel(real_ns, "TURTLE");
			packageEx = getParent().toPackageEx(model.getKey(), model.getValue(), false);
		}
		return new AbstractMap.SimpleEntry<PackageEx, Resource>(packageEx,
				packageEx.getModel().getResource(resource.getURI()));
	}

	public TypeEx getType(String id) {
		return annoymous.get(id);
	}

	public ClassEx toClassEx(Resource resource) {
		Entry<PackageEx, Resource> real = getRealResource(resource);
		PackageEx real_package = real.getKey();
		String local = resource.getLocalName();
		if (real_package != this)
			return real_package.toClassEx(local, real.getValue());
		return toClassEx(local, resource);
	}

	public Set<FieldEx> toFieldEx(Resource resource) {
		Entry<PackageEx, Resource> real = getRealResource(resource);
		PackageEx real_package = real.getKey();
		String local = resource.getLocalName();
		if (real_package != this)
			return real_package.toFieldEx(local, real.getValue());
		return toFieldEx(local, resource);
	}

	public FieldEx toFieldEx(Resource resource, ClassEx cls) {
		Entry<PackageEx, Resource> real = getRealResource(resource);
		PackageEx real_package = real.getKey();
		String local = resource.getLocalName();
		if (real_package != this)
			real_package.toFieldEx(local, real.getValue(), cls);
		return toFieldEx(local, resource, cls);
	}

	private void fillClassEx(Resource domain, ClassEx cls) {
		Model model = domain.getModel();
		for (Resource property : RDFSUtils.getProperties(model)) {
			Set<Resource> domains = RDFSUtils.getDomains(property);
			if (domains.contains(domain))
				toFieldEx(property, cls);
		}
	}

	private ClassEx getSuperClass(Resource resource) {
		Resource superClass = RDFSUtils.getSuperClass(resource);
		if (superClass != null && !superClass.equals(resource)) {
			Entry<PackageEx, Resource> real = getRealResource(superClass);
			Resource realSuperClass = real.getValue();
			PackageEx realPackage = real.getKey();
			return realPackage.toClassEx(realSuperClass.getLocalName(), realSuperClass);
		}
		return null;
	}

	private Type getType(Set<TypeEx> types) {
		if (types == null)
			return null;
		if (types.size() == 1)
			return types.iterator().next().getType();
		String hash = getTypesHash(types);
		Interface result = types2interface.get(hash);
		if (result != null)
			return result;
		Set<Package> pack = new HashSet<Package>();
		List<String> names = new ArrayList<String>();
		result = new Interface();
		for (TypeEx type : types) {
			Type real = type.getType();
			if (real instanceof Class) {
				Class cls = (Class) real;
				pack.add(cls.getContainer());
				names.add(cls.getName());
				cls.addInterface(result);
			}
		}
		if (pack.isEmpty())
			return null;
		if (pack.size() == 1)
			result.setContainer(pack.iterator().next());
		else
			result.setContainer(Constants.DEFAULT_PACKAGE.getPackage());
		result.setName(String.join("_", names));
		types2interface.put(hash, result);
		return result;
	}

	private String getTypesHash(Set<TypeEx> types) {
		Set<String> str = new HashSet<String>();
		types.forEach(type -> str.add(type.getSource().asResource().getURI()));
		List<String> list = new ArrayList<String>();
		list.addAll(str);
		Collections.sort(list);
		return String.join(";", list);
	}

	private ClassEx toClassEx(String id, Resource resource) {
		ClassEx type = getClassEx(id);
		if (type == null) {
			type = createClass(id, resource);
			fillClassEx(resource, type);
			type.setSuperClass(getSuperClass(resource));
		}
		return type;
	}

	/**
	 * Tranform a resource into field
	 * 
	 * @param resource
	 *            the property to be transformed
	 * @param packEx
	 * @return transformed fields
	 */
	private Set<FieldEx> toFieldEx(String local, Resource resource) {
		// find the domains of the property
		Set<ClassEx> domains = getDomains(resource);
		if (domains == null)
			return null;
		Set<FieldEx> fields = new HashSet<FieldEx>();
		// find the ranges of the property
		Set<TypeEx> ranges = getRanges(resource);
		// find the type of the ranges
		Type rangeType = getType(ranges);
		/**
		 * create a field in each of the found domains
		 */
		for (ClassEx domain : domains) {
			FieldEx field = domain.toFieldEx(local, resource);
			if (field != null) {
				// set the ranges of the field
				field.setType(ranges, rangeType);
				fields.add(field);
			}
		}
		return fields;
	}

	private FieldEx toFieldEx(String local, Resource resource, ClassEx cls) {
		List<FieldEx> fields = new ArrayList<FieldEx>();
		// find the ranges for the property
		Set<TypeEx> ranges = getRanges(resource);
		// find the type for the range
		Type rangeType = getType(ranges);
		/**
		 * create a field in each of the found domains
		 */
		FieldEx field = cls.toFieldEx(local, resource);
		if (field != null) {
			// set the ranges of the field
			field.setType(ranges, rangeType);
			fields.add(field);
		}
		return field;
	}

}
