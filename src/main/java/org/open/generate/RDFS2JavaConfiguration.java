package org.open.generate;

import java.util.HashMap;
import java.util.Map;

public class RDFS2JavaConfiguration {

	/*
	 * Local Path to model
	 */
	private Map<String, PackageEx> path2package = new HashMap<String, PackageEx>();

	public String getFieldName(String local) {
		local = local.replaceAll("-", "_");
		if (local.equals("abstract") || local.equals("implements") || local.matches("[0-9].*"))
			return "_" + local;
		return local;

	}

	public PackageEx getLocal(String path) {
		return path2package.get(path);
	}

	/**
	 * register the package for the local path {@code local_path}
	 * 
	 * @param local_path
	 *            the local path
	 * @param pack
	 *            the package to be registered
	 */
	public void registerLocal(String local_path, PackageEx pack) {
		if (!local_path.equals(pack.getPackage().getName())) {
			path2package.put(local_path, pack);
		}

	}

	// public void registerPackageEx(String namespace, PackageEx result) {
	// ns2package.put(namespace, result);
	// }
}
