package org.open.generate;

import java.util.HashMap;
import java.util.Map;

public class RDFS2JavaConfiguration {

//	public static Package DEFAULT_PACKAGE = new Package("utils.generate.org");
//
//	/**
//	 * The hash table for generate packages<br>
//	 * The generated packages are hashed based on their namespace
//	 */
//	private Map<String, PackageEx> ns2package = new HashMap<String, PackageEx>();
//
//	{
//		ns2package.put("EMPTY", new PackageEx(null, DEFAULT_PACKAGE, null));
//	}
//
//	/**
//	 * @param namespace
//	 * @return the generated package using the key {@code namespace}
//	 */
//	public PackageEx getPackageEx(String namespace) {
//		return ns2package.get(namespace);
//	}
//
//	/**
//	 * 
//	 * @return the hash table for generate packages {@link #ns2package}
//	 */
//	public Map<String, PackageEx> getHash() {
//		return ns2package;
//	}

	/*
	 * Local Path to model
	 */
	private Map<String, PackageEx> path2package = new HashMap<String, PackageEx>();

	/**
	 * register the package for the local path {@code local_path}
	 * @param local_path the local path
	 * @param pack the package to be registered
	 */
	public void registerLocal(String local_path, PackageEx pack) {
		if (!local_path.equals(pack.getPackage().getName())) {
			path2package.put(local_path, pack);
		}

	}

	public PackageEx getLocal(String path) {
		return path2package.get(path);
	}

	public String getFieldName(String local) {
		return "_" + local;
	}

//	public void registerPackageEx(String namespace, PackageEx result) {
//		ns2package.put(namespace, result);
//	}
}
