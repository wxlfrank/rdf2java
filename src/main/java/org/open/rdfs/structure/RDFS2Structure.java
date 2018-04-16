package org.open.rdfs.structure;

import java.util.Collection;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.open.rdfs.RDFSFile;
import org.open.rdfs.java.Constants;

public class RDFS2Structure extends Binding implements Configurable {

	private RDFSExConfig config = new RDFSExConfig();

	/**
	 * create a transformation from RDFS/OWL to java classes from a set of local
	 * RDFS/OWL files
	 * 
	 * @param local_files
	 *            the local RDFS/OWL files
	 */
	public RDFS2Structure(List<RDFSFile> local_files) {
		super(null, null, null);
		if (local_files != null) {
			local_files.forEach(local -> translate(local, false));
		}
	}
	public RDFS2Structure() {}

	public PackageEx createPackageEx(RDFSFile rdfs) {
		return new PackageEx(this, rdfs.getNamespace(), rdfs.getModel());
	}

	public void supplementPackage(PackageEx packageEx) {
		Model model = packageEx.getModel();
		RDFSUtilWithCache cache = RDFSUtilWithCache.INSTANCE;
		for (Resource rdfsClass : cache.getClasses(model)) {
			packageEx.toClassEx(rdfsClass);
		}
		// for those properties without domains
//		for (Resource rdfsProperty : cache.getProperties(model)) {
//			packageEx.toFieldEx(rdfsProperty);
//		}
		packageEx.handleShape();
	}

	public RDFSExConfig getConfiguration() {
		return config;
	}

	@Override
	public String getHashString() {
		return null;
	}

	/**
	 * Get the package corresponding to the namespace
	 * 
	 * @param namespace
	 * @return
	 */
	public PackageEx getPackageEx(String namespace) {
		return (PackageEx) getContents().get(namespace);
	}

	public Collection<Binding> getPackageExes() {
		return getContents().values();
	}

	/**
	 * translate a model with namespace to a {@link PackageEx}
	 * 
	 * @param namespace
	 * @param model
	 * @param translateAll
	 * @return
	 */
	public PackageEx toPackageEx(String namespace, Model model, boolean translateAll) {
		PackageEx packageEx = getPackageEx(namespace);
		// If no package is found for the namespace, create a new package for the
		// namespace
		if (packageEx == null) {
			boolean isXMLSchema = namespace.equals(Constants.XMLSchema_URI);
			packageEx = isXMLSchema ? Constants.XMLSchema_Package : new PackageEx(this, namespace, model);
			if (!isXMLSchema && translateAll)
				supplementPackage(packageEx);
		}
		return packageEx;
	}

	/**
	 * The entry of translation. Transform a {@link RDFSFile} to corresponding
	 * uml-like class diagram
	 * <ol>
	 * <li>Transform all the properties to fileds</li>
	 * </ol>
	 * 
	 * @param rdfsFile
	 *            the local file
	 * @return all the {@link PackageEx}s generated
	 */

	public Collection<Binding> translate(RDFSFile rdfsFile) {
		translate(rdfsFile, true);
		return getPackageExes();
	}

	/**
	 * Translate a {@link RDFSFile} {@code rdfs} to a package
	 * 
	 * @param rdfs
	 *            a {@link RDFSFile}
	 * @param translateAll
	 *            translate all the content in the contained model if it is true.
	 */
	private void translate(RDFSFile rdfs, boolean translateAll) {
		if (rdfs.getModel() == null) {
			config.readWriteModel(rdfs);
			toPackageEx(rdfs.getNamespace(), rdfs.getModel(), translateAll);
		}
	}
}
