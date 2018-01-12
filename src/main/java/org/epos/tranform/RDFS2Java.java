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

public class RDFS2Java {

	private RDFS2JavaConfiguration configuration = new RDFS2JavaConfiguration();

	private JavaGenerator generator = new JavaGenerator(configuration);

	/**
	 * create a transformation from RDFS/OWL to java classes from a set of local
	 * RDFS/OWL files
	 * 
	 * @param local_files
	 *            the local RDFS/OWL files
	 */
	public RDFS2Java(List<RDFFile> local_files) {
		if (local_files != null) {
			local_files.forEach(local -> readLocalFile(local, false));
		}
	}

	private void readLocalFile(RDFFile local, boolean translate) {
		String path = local.path;
		PackageEx ex = configuration.getLocal(path);
		if (ex == null) {
			ex = toPackageEx(path, local.format, translate);
			configuration.registerLocal(path, ex);
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
	 *            the namespace
	 * @param format
	 *            the format of the model to be read
	 * @param translate
	 */
	public PackageEx toPackageEx(String namespace, String format, boolean translate) {
		PackageEx result = configuration.getPackageEx(namespace);
		if (result == null) {
			if(Constants.XMLSchema_URI.startsWith(namespace))
			System.out.println("*************************" + namespace);
			if (namespace.equals(Constants.XMLSchema_URI)) {
				result = Constants.XMLSchema_Package;
				configuration.getHash().put(namespace, result);
				return result;
			}
			Map.Entry<String, Model> model = RDFSUtils.readWriteModel(namespace, format);
			// System.out.println(namespace + "->" + model.getKey());
			result = configuration.createPackageEx(model.getKey(), model.getValue());
			if (translate)
				translatePackage(result);
		}
		return result;
	}

	public FieldEx createFieldEx(Resource resource, ClassEx domain) {
		FieldEx result = domain.createField(resource);
		List<TypeEx> ranges = getRange(resource, domain.getParent());
		result.setType(ranges, getType(ranges));
		return result;
	}

	public void translatePackage(PackageEx pack) {
		Model model = pack.getModel();
		// Set<Resource> classes = RDFSUtils.getClasses(model);
		// classes.forEach(resource -> {
		// toClassEx(resource, pack);
		// });
		for (Resource property : RDFSUtils.getProperties(model)) {
			toFieldEx(property, pack);
		}
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
		// PackageEx pack = configuration.getLocal(local_file.path);
		// translatePackage(pack);
		// String ns = pack.getPackage().getName();
		// model.listOntProperties().forEachRemaining(property -> {
		// if (property.getNameSpace().equals(ns)) {
		// toFieldEx(property, pack);
		// }
		// // Map.Entry<Resource, PackageEx> real = getRealResource(property, pack);
		// });
		// try {
		// model.write(new FileWriter("result.ttl"), "TURTLE");
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// model.listSubjectsWithProperty(RDF.type,
		// OWL.ObjectProperty).forEachRemaining(property -> {
		// Map.Entry<Resource, PackageEx> real = getRealResource(property, pack);
		// translate2Field(real.getKey(), real.getValue());
		// });
		generator.generate(Paths.get("src/main/java").toFile());
	}

	private ClassEx getSuperClass(Resource resource, PackageEx pack, boolean fill) {
		Resource superClass = RDFSUtils.getSuperClass(resource);
		if (superClass != null && !superClass.equals(resource)) {
			Entry<Resource, PackageEx> real = getRealResource(superClass, pack);
			Resource realSuperClass = real.getKey();
			PackageEx realPackage = real.getValue();
			return toClassEx(realSuperClass, realPackage, fill || pack != realPackage);
		}
		return null;
	}

	/*
	 * create a class from a resource
	 */
	public ClassEx toClassEx(Resource resource, PackageEx pack, boolean fill) {
		String id = resource.getLocalName();
		ClassEx type = pack.getClass(id);
		if (type == null) {
			type = pack.createClass(id, resource);
			if (fill)
				fillClassEx(resource, pack, type);
			type.setSuperClass(getSuperClass(resource, pack, fill));
		}
		return type;
	}

	public List<TypeEx> getRange(Resource resource, PackageEx pack) {
		List<TypeEx> result = new ArrayList<TypeEx>();
		if (resource instanceof Resource) {
			Set<Resource> ranges = RDFSUtils.getRanges(resource);
			if (!ranges.isEmpty()) {
				for (Resource range : ranges) {
					if (range.isAnon()) {
						String id = range.getId().toString();
						TypeEx type = pack.getType(id);
						if (type == null) {
							Set<Resource> types = RDFSUtils.getTypes(range);
							boolean isclass = RDFSUtils.isClass(types);
							if (isclass) {
								List<Resource> union = RDFSUtils.getUnionOf(range);
								if (union != null) {
									for (Resource iter : union) {
										result.add(toClassEx(iter, pack, false));
									}
								}
							}
						} else
							result.add(type);
					} else {
						Entry<Resource, PackageEx> real = getRealResource(range, pack);
						result.add(toClassEx(real.getKey(), real.getValue(), pack != real.getValue()));
					}
				}
				return result;
			} else {
				Statement stm_subPropertyOf = resource.getProperty(RDFS.subPropertyOf);
				if (stm_subPropertyOf != null) {
					Resource sub = stm_subPropertyOf.getObject().asResource();
					Entry<Resource, PackageEx> real = getRealResource(sub, pack);
					List<FieldEx> fields = toFieldEx(real.getKey(), real.getValue());
					if (!fields.isEmpty()) {
						return fields.get(0).getType();
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

	private Type getType(List<TypeEx> types) {
		if (types == null)
			return null;
		if (types.size() == 1)
			return types.get(0).getType();
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
		if (pack.size() == 0)
			result.setContainer(pack.iterator().next());
		else
			result.setContainer(RDFS2JavaConfiguration.DEFAULT_PACKAGE);
		result.setName(String.join("_", names));
		types2interface.put(hash, result);
		return result;
	}

	private String getTypesHash(List<TypeEx> types) {
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
	 * @param pack
	 * @return transformed fields
	 */
	private List<FieldEx> toFieldEx(Resource resource, PackageEx pack) {
		// find the domains for the property
		List<ClassEx> domains = getDomain(resource, pack);
		if (domains == null)
			return null;
		List<FieldEx> fields = new ArrayList<FieldEx>();
		// find the ranges for the property
		List<TypeEx> ranges = getRange(resource, pack);
		// find the type for the range
		System.out.println("translate resource " + resource.getURI() + " to field");
		Type rangeType = getType(ranges);
		/**
		 * create a field in each of the found domains
		 */
		for (ClassEx domain : domains) {
			FieldEx field = domain.getField(resource.getLocalName());
			if (field == null) {
				field = domain.createField(resource);
			}
			if (field != null) {
				// set the ranges of the field
				field.setType(ranges, rangeType);
				fields.add(field);
			}
		}
		return fields;
	}

	private List<ClassEx> getDomain(Resource resource, PackageEx pack) {
		if (resource instanceof Resource) {
			List<ClassEx> results = new ArrayList<ClassEx>();
			Set<Resource> domains = RDFSUtils.getDomains(resource);
			if (!domains.isEmpty()) {
				for (Resource domain : domains) {
					Entry<Resource, PackageEx> real = getRealResource(domain, pack);
					Resource realDomain = real.getKey();
					PackageEx realPackage = real.getValue();
					ClassEx cls = toClassEx(realDomain, realPackage, pack != realPackage);
					results.add(cls);

				}
			} else {
				Resource sub = RDFSUtils.getSubProperty(resource);
				if (sub != null) {
					Entry<Resource, PackageEx> real = getRealResource(sub, pack);
					List<FieldEx> fields = toFieldEx(real.getKey(), real.getValue());
					fields.forEach(field -> {
						results.add(field.getClassEx());
					});
				}
			}
			return results;
		}
		return null;
	}

	private void fillClassEx(Resource domain, PackageEx pack, ClassEx cls) {
		Model model = domain.getModel();
		for (Resource property : RDFSUtils.getProperties(model)) {
			Set<Resource> domains = RDFSUtils.getDomains(property);
			if (domains.contains(domain))
				toFieldEx(property, pack, cls);
		}
	}

	private FieldEx toFieldEx(Resource resource, PackageEx pack, ClassEx cls) {
		List<FieldEx> fields = new ArrayList<FieldEx>();
		// find the ranges for the property
		List<TypeEx> ranges = getRange(resource, pack);
		// find the type for the range
		Type rangeType = getType(ranges);
		/**
		 * create a field in each of the found domains
		 */
		FieldEx field = cls.getField(resource.getLocalName());
		if (field == null) {
			field = cls.createField(resource);
		}
		if (field != null) {
			// set the ranges of the field
			field.setType(ranges, rangeType);
			fields.add(field);
		}
		return field;
	}

	private Map.Entry<Resource, PackageEx> getRealResource(Resource resource, PackageEx packEx) {
		String real_ns = resource.getNameSpace();
		if (real_ns == null)
			return new AbstractMap.SimpleEntry<Resource, PackageEx>(resource, packEx);
		real_ns = RDFSUtils.unifyNS(real_ns);
		String ns = packEx.getPackage().getName();
		if (ns.equals(real_ns))
			return new AbstractMap.SimpleEntry<Resource, PackageEx>(resource, packEx);
		PackageEx real_pack = toPackageEx(real_ns, "TURTLE", true);
		Model real_model = real_pack.getModel();
		return new AbstractMap.SimpleEntry<Resource, PackageEx>(real_model.getResource(resource.getURI()), real_pack);
	}

	public RDFS2JavaConfiguration getConfiguration() {
		return configuration;
	}

}
