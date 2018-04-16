package org.open.rdfs.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.open.structure.Package;
import org.open.structure.Type;

public class JavaType extends Type {

	private static final Map<String, Package> JAVA_PACKAGES = new HashMap<String, Package>();
	public static final JavaType LIST = new JavaType(List.class);
	public static final JavaType ARRAYLIST = new JavaType(ArrayList.class);

	public JavaType(Class<?> clazz) {
		this.setContainer(getJavaPackage(clazz.getPackage().getName()));
		this.setName(clazz.getSimpleName());
	}

	private Package getJavaPackage(String name) {
		Package result = JAVA_PACKAGES.get(name);
		if (result == null) {
			result = new Package(name);
			JAVA_PACKAGES.put(name, result);
		}
		return result;
	}
}
