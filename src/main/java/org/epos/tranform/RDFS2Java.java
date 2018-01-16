package org.epos.tranform;

import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDFS;
import org.open.generate.Binding;
import org.open.generate.Class;
import org.open.generate.ClassEx;
import org.open.generate.Constants;
import org.open.generate.DataTypeHash;
import org.open.generate.FieldEx;
import org.open.generate.Interface;
import org.open.generate.JavaGenerator;
import org.open.generate.Package;
import org.open.generate.PackageEx;
import org.open.generate.RDFS2JavaConfiguration;
import org.open.generate.RDFSUtils;
import org.open.generate.Type;
import org.open.generate.TypeEx;

public class RDFS2Java extends Binding {

	private RDFS2JavaConfiguration configuration = new RDFS2JavaConfiguration();

	private JavaGenerator generator = new JavaGenerator(this, configuration);

	/**
	 * create a transformation from RDFS/OWL to java classes from a set of local
	 * RDFS/OWL files
	 * 
	 * @param local_files
	 *            the local RDFS/OWL files
	 */
	public RDFS2Java(List<RDFFile> local_files) {
		super(null, null, null);
		if (local_files != null) {
			local_files.forEach(local -> readLocalFile(local, false));
		}
	}

	private void readLocalFile(RDFFile local, boolean translate) {
		String path = local.path;
		if (configuration.getLocal(path) == null) {
			Entry<String, Model> ns2model = RDFSUtils.readWriteModel(path, local.format);
			String ns = ns2model.getKey();
			Model model = ns2model.getValue();
			configuration.registerLocal(path, toPackageEx(ns, model, translate));
		}
	}

	/**
	 * Returns the generated package for a given {@code namespace} <br>
	 * 1. search for a generated package from the hash table for the generated
	 * package {@link RDFS2JavaConfiguration#getHash()} using the key
	 * {@code namespace}<br>
	 * 2. If a generated package is found, return the package <br>
	 * 3. Otherwise, read the model corresponding to the {@code namespace}, hash the
	 * new model and return the model
	 * 
	 * @param namespace
	 *            the namespace of the model
	 * @param model
	 *            the model to be translated
	 * @param translate
	 */
	public PackageEx toPackageEx(String namespace, Model model, boolean translate) {
		PackageEx result = getPackageEx(namespace);
		if (result == null) {
			boolean isXMLSchema = namespace.equals(Constants.XMLSchema_URI);
			result = isXMLSchema ? Constants.XMLSchema_Package : createPackageEx(namespace, model);
			if (!isXMLSchema && translate)
				fillPackageEx(result);
		}
		return result;
	}

	public PackageEx getPackageEx(String namespace) {
		return (PackageEx) getChildren().get(namespace);
	}

	/**
	 * @param namespace
	 * @param model
	 * @return a newly created package using the {@code namespace} and the
	 *         {@code model}
	 */
	public PackageEx createPackageEx(String namespace, Model model) {
		Package pack = new Package(namespace);
		PackageEx packEx = new PackageEx(this, model, pack);
		return packEx;
	}

	// public FieldEx createFieldEx(Resource resource, ClassEx domain) {
	// FieldEx result = domain.createField(resource);
	// Set<TypeEx> ranges = getRanges(domain.getParent(), resource);
	// result.setType(ranges, getType(ranges));
	// return result;
	// }

	public void fillPackageEx(PackageEx pack) {
		Model model = pack.getModel();
		for (Resource property : RDFSUtils.getProperties(model)) {
			toFieldEx(pack, property);
		}
		// for (Resource property : RDFSUtils.getNodeShape(model)) {
		// toFieldEx(property, pack);
		// }
	}

	/**
	 * Transform a local file to java code
	 * <ol>
	 * <li>Transform all the properties to fileds</li>
	 * </ol>
	 * 
	 * @param local_file
	 *            the local file
	 */

	public void translate(RDFFile local_file) {
		readLocalFile(local_file, true);
		generator.generate(Paths.get("src/main/java").toFile());
	}

	// private ClassEx getSuperClass(Resource resource, PackageEx pack, boolean
	// fill) {
	// Resource superClass = RDFSUtils.getSuperClass(resource);
	// if (superClass != null && !superClass.equals(resource)) {
	// Entry<PackageEx, Resource> real = getRealResource(pack, superClass);
	// PackageEx realPackage = real.getKey();
	// Resource realSuperClass = real.getValue();
	// return toClassEx(realSuperClass, realPackage, fill || pack != realPackage);
	// }
	// return null;
	// }

	/*
	 * create a class from a resource
	 */
	// public ClassEx toClassEx(Resource resource, PackageEx pack, boolean fill) {
	// String id = resource.getLocalName();
	// ClassEx type = pack.getClass(id);
	// if (type == null) {
	// type = pack.createClass(id, resource);
	// if (fill)
	// fillClassEx(resource, pack, type);
	// type.setSuperClass(getSuperClass(resource, pack, fill));
	// }
	// return type;
	// }

	/**
	 * Retrieve the ranges of the resource. There may exist a collection of ranges
	 * 
	 * @param packEx
	 * @param resource
	 * @return the ranges of the resource
	 */
	public Set<TypeEx> getRanges(PackageEx packEx, Resource resource) {
		Set<TypeEx> result = new HashSet<TypeEx>();
		if (resource instanceof Resource) {
			Set<Resource> ranges = RDFSUtils.getRanges(resource);
			if (!ranges.isEmpty()) {
				for (Resource range : ranges) {
					if (range.isAnon()) {
						String id = range.getId().toString();
						TypeEx type = packEx.getType(id);
						if (type == null) {
							Set<Resource> types = RDFSUtils.getTypes(range);
							boolean isclass = RDFSUtils.isClass(types);
							if (isclass) {
								List<Resource> union = RDFSUtils.getUnionOf(range);
								if (union != null) {
									for (Resource iter : union) {
										result.add(packEx.toClassEx(iter.getLocalName(), iter, false));
									}
								}
							}
						} else
							result.add(type);
					} else {
						if(resource.getURI().equals("http://www.w3.org/ns/dcat#bytes"))
							System.out.println(resource.getURI());
						Entry<PackageEx, Resource> real = getRealResource(packEx, range);
						PackageEx realPackage = real.getKey();
						Resource realSuperClass = real.getValue();
						result.add(realPackage.toClassEx(realSuperClass.getLocalName(), realSuperClass,
								packEx != real.getValue()));
					}
				}
				return result;
			} else {
				Statement stm_subPropertyOf = resource.getProperty(RDFS.subPropertyOf);
				if (stm_subPropertyOf != null) {
					Resource sub = stm_subPropertyOf.getObject().asResource();
					Entry<PackageEx, Resource> real = getRealResource(packEx, sub);
					Set<FieldEx> fields = toFieldEx(real.getKey(), real.getValue());
					if (!fields.isEmpty()) {
						return fields.iterator().next().getTypes();
					}
				}
			}
		}
		result.add(Constants.XMLSchema_Package.getClass("string"));
		return result;
	}

	// private Field translate2StaticField(Resource domain, Resource resource) {
	// ClassHash cls_hash = translate2Class(domain);
	// String local = resource.getLocalName();
	// Field result = cls_hash.getField(local);
	// if (result != null)
	// return result;
	// /**
	// * Get Domain 1. resource has rdf:domain property 2. resource has
	// * rdf:subPropertyOf property
	// */
	//
	// result = new Field(cls_hash.getCls(), local);
	// cls_hash.addField(local, result);
	// result.setType(cls_hash.getCls());
	// return result;
	// }

	protected DataTypeHash getDataType(Resource resource) {
		return null;
	}

	// protected void handleSuperClass(Class cls, Resource resource) {
	// Statement subclass = resource.getProperty(RDFS.subClassOf);
	// if (subclass != null) {
	// Resource superClass = subclass.getObject().asResource();
	// Resource real_superClass = getRealResource(superClass,
	// unifyNS(resource.getNameSpace()));
	// ClassHash hash = getClass(real_superClass);
	// Class superCls = hash.getCls();
	// if (superCls == null) {
	// superCls = createClass(real_superClass, hash);
	// if (superCls != null)
	// cls.setSuperClass(superCls);
	// }
	// }
	// }

	private Map<String, Interface> types2interface = new HashMap<String, Interface>();

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

	/**
	 * Tranform a resource into field
	 * 
	 * @param resource
	 *            the property to be transformed
	 * @param packEx
	 * @return transformed fields
	 */
	private Set<FieldEx> toFieldEx(PackageEx packEx, Resource resource) {
		// find the domains for the property
		Set<ClassEx> domains = getDomains(packEx, resource);
		if (domains == null)
			return null;
		Set<FieldEx> fields = new HashSet<FieldEx>();
		// find the ranges for the property
		Set<TypeEx> ranges = getRanges(packEx, resource);
		// find the type for the range
		System.out.println("translate resource " + resource.getURI() + " to field");
		Type rangeType = getType(ranges);
		/**
		 * create a field in each of the found domains
		 */
		for (ClassEx domain : domains) {
			FieldEx field = domain.toFieldEx(resource.getLocalName(), resource);
			if (field != null) {
				// set the ranges of the field
				field.setType(ranges, rangeType);
				fields.add(field);
			}
		}
		return fields;
	}

	/**
	 * Retrieve the domains of the resource; there maybe exists a collection of
	 * resources which are the domains of the resource
	 * 
	 * @param packEx
	 * @param resource
	 *            the resource
	 * @return the domains of the resource
	 */
	private Set<ClassEx> getDomains(PackageEx packEx, Resource resource) {
		if (resource instanceof Resource) {
			Set<ClassEx> results = new HashSet<ClassEx>();
			Set<Resource> domains = RDFSUtils.getDomains(resource);
			if (!domains.isEmpty()) {
				for (Resource domain : domains) {
					Entry<PackageEx, Resource> real = getRealResource(packEx, domain);
					PackageEx realPackage = real.getKey();
					Resource realDomain = real.getValue();
					ClassEx cls = realPackage.toClassEx(realDomain.getLocalName(), realDomain, packEx != realPackage);
					results.add(cls);
				}
			} else {
				Resource sub = RDFSUtils.getSubProperty(resource);
				if (sub != null) {
					Entry<PackageEx, Resource> real = getRealResource(packEx, sub);
					toFieldEx(real.getKey(), real.getValue()).forEach(field -> {
						results.add(field.getClassEx());
					});
				}
			}
			return results;
		}
		return null;
	}

	public FieldEx toFieldEx(PackageEx pack, Resource resource, ClassEx cls) {
		List<FieldEx> fields = new ArrayList<FieldEx>();
		// find the ranges for the property
		Set<TypeEx> ranges = getRanges(pack, resource);
		// find the type for the range
		Type rangeType = getType(ranges);
		/**
		 * create a field in each of the found domains
		 */
		FieldEx field = cls.toFieldEx(resource.getLocalName(), resource);
		if (field != null) {
			// set the ranges of the field
			field.setType(ranges, rangeType);
			fields.add(field);
		}
		return field;
	}

	/**
	 * find the real resource of the {@code resource}. The real resource may be the
	 * same as the resource or exists in another model, but has the same URI as the
	 * resource.
	 * 
	 * @param packEx
	 *            the container to find the real resource
	 * @param resource
	 * @return
	 */
	public Map.Entry<PackageEx, Resource> getRealResource(PackageEx packEx, Resource resource) {
		String real_ns = resource.getNameSpace();
		if (real_ns == null)
			return new AbstractMap.SimpleEntry<PackageEx, Resource>(packEx, resource);
		real_ns = RDFSUtils.unifyNS(real_ns);
		String ns = packEx.getPackage().getName();
		if (ns.equals(real_ns))
			return new AbstractMap.SimpleEntry<PackageEx, Resource>(packEx, resource);
		Entry<String, Model> model = RDFSUtils.readWriteModel(real_ns, "TURTLE");
		PackageEx real_pack = toPackageEx(model.getKey(), model.getValue(), true);
		Model real_model = real_pack.getModel();
		return new AbstractMap.SimpleEntry<PackageEx, Resource>(real_pack, real_model.getResource(resource.getURI()));
	}

	public RDFS2JavaConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public String getHashString() {
		return null;
	}

}
