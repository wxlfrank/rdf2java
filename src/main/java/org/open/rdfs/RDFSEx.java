package org.open.rdfs;

import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

public class RDFSEx extends Binding {

	private RDFSExConfig config = new RDFSExConfig();

	/**
	 * create a transformation from RDFS/OWL to java classes from a set of local
	 * RDFS/OWL files
	 * 
	 * @param local_files
	 *            the local RDFS/OWL files
	 */
	public RDFSEx(List<RDFSFile> local_files) {
		super(null, null, null);
		if (local_files != null) {
			local_files.forEach(local -> readLocalFile(local, false));
		}
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

	public void fillPackageEx(PackageEx packEx) {
		Model model = packEx.getModel();
		for (Resource property : RDFSUtils.getProperties(model)) {
			packEx.toFieldEx(property);
		}
		for (Resource shape : RDFSUtils.getNodeShapes(model)) {
			Resource cls = RDFSUtils.getTargetClass(shape);
			if (cls != null) {
				ClassEx classEx = packEx.toClassEx(cls);
				RDFSUtils.getShaclProperties(shape).forEach(property -> {
					Resource path = RDFSUtils.getPath(property);
					FieldEx fieldEx = packEx.toFieldEx(path, classEx);
					Resource kind = RDFSUtils.getNodeKind(property);
					if (kind == null)
						kind = RDFSUtils.getShaclClass(property);
					if (kind != null) {
						if (kind.getLocalName().equals("BlankNodeOrLiteral") && path.getLocalName().equals("keyword"))
							System.out.println("this ");
						if (kind.equals(SHACL.IRIOrLiteral) || kind.equals(SHACL.Literal)) {
							fieldEx.setTypeEx(Constants.XMLSchema_Package.getClassEx("string"));
						} else
							fieldEx.setTypeEx(packEx.toClassEx(kind));
					}
					Literal maxCount = RDFSUtils.getMaxCount(property);
					Integer count = null;
					try {
						count = maxCount == null ? Integer.MAX_VALUE : Integer.parseInt(maxCount.toString());
					} catch (NumberFormatException e) {
						count = Integer.MAX_VALUE;
					}
					fieldEx.getField().setMultiplicity(count);
				});
			}
		}
	}

	public RDFSExConfig getConfig() {
		return config;
	}

	@Override
	public String getHashString() {
		return null;
	}

	public PackageEx getPackageEx(String namespace) {
		return (PackageEx) getChildren().get(namespace);
	}

	public Collection<Binding> getPackageExes() {
		return getChildren().values();
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

	/**
	 * Returns the generated package for a given {@code namespace} <br>
	 * 1. search for a generated package from the hash table for the generated
	 * package {@link RDFSExConfig#getHash()} using the key {@code namespace}<br>
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

	/**
	 * Transform a local file to java code
	 * <ol>
	 * <li>Transform all the properties to fileds</li>
	 * </ol>
	 * 
	 * @param local_file
	 *            the local file
	 * @return
	 */

	public Collection<Binding> translate(RDFSFile local_file) {
		readLocalFile(local_file, true);
		return getPackageExes();
	}

	private void readLocalFile(RDFSFile local, boolean translate) {
		String path = local.path;
		if (config.getLocal(path) == null) {
			Entry<String, Model> ns2model = config.readWriteModel(path, local.format);
			String ns = ns2model.getKey();
			Model model = ns2model.getValue();
			config.registerLocal(path, toPackageEx(ns, model, translate));
		}
	}

}
