package org.open.rdfs.java;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.open.structure.Class;
import org.open.structure.Interface;
import org.open.structure.Package;
import org.open.structure.Type;

public class JavaUtil {
	private static final int __0x20 = ~(0x20);

	private static final String VARIABLE_NAME_PATTERN = "^[\\$_a-zA-Z]+[\\$_\\w]*$";

	public static String captializeFirstChar(String name) throws NullPointerException {
		checkNull(name, "It is illegal to captialize a null value.");
		if (name.isEmpty())
			return name;
		return (char) (name.charAt(0) & __0x20) + name.substring(1);
	}

	public static void checkJavaVariable(String variable) throws NullPointerException, IllegalStateException {
		checkNull(variable, "varible is a null");
		if (!variable.matches(VARIABLE_NAME_PATTERN))
			throw new IllegalStateException("variable is not valid identifier");
	}

	public static void checkNull(String variable, String exceptionMesage) {
		if (variable == null)
			throw new NullPointerException(exceptionMesage);
	}

	public static String getGetName(String name) throws NullPointerException, IllegalStateException {
		checkJavaVariable(name);
		return "get" + captializeFirstChar(name);
	}

	public static String getSetName(String name) throws NullPointerException, IllegalStateException {
		checkJavaVariable(name);
		return "set" + captializeFirstChar(name);
	}
	
	
	
	public static String getTypesHash(Set<? extends Type> types) {
		Set<String> str = new LinkedHashSet<String>();
		types.forEach(type -> str.add(type.toString()));
		List<String> list = new ArrayList<String>();
		list.addAll(str);
		Collections.sort(list);
		return String.join(";", list);
	}
	
	static Interface getInterfaceOfClasses(Set<? extends Type> types) {
		if (types == null)
			return null;
		Set<Package> pack = new LinkedHashSet<Package>();
		List<String> names = new ArrayList<String>();
		Interface itf = new Interface();
		for (Type type : types) {
			if (type instanceof Class) {
				Class clazz = (Class) type;
				pack.add(clazz.getContainer());
				names.add(clazz.getName());
				clazz.addInterface(itf);
			}
		}
		if (pack.isEmpty())
			return null;
		if (pack.size() == 1)
			itf.setContainer(pack.iterator().next());
		else
			itf.setContainer(Constants.DEFAULT_PACKAGE.getPackage());
		itf.setName(JavaUtil.captializeFirstChar(String.join("_", names)));
		return itf;
	}
	
	public static boolean isCollectionClass(java.lang.Class<?> clazz) {
		return Collection.class.isAssignableFrom(clazz);
	}
}
