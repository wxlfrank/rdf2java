package org.open.structure;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Util {
	public static Set<Type> getAllParents(Class clazz) {
		Set<Type> results = new LinkedHashSet<Type>();
		Set<Class> parents = clazz.getParents();
		results.addAll(parents);
		for (Class iter : parents) {
			results.addAll(getAllParents(iter));
		}
		return results;
	}

	public static Class getCommonParent(Set<? extends Type> classes) {
		for (Type iter1 : classes) {
			if (!(iter1 instanceof Class))
				continue;
			boolean result = false;
			for (Type iter2 : classes) {
				if (!(iter2 instanceof Class))
					continue;
				result = isSuperClassOf((Class) iter1, (Class) iter2);
				if (result == false)
					break;
			}
			if (result == true)
				return (Class) iter1;
		}
		return null;
	}

	static private Map<String, Boolean> checkHash = new HashMap<String, Boolean>();

	public static boolean isSuperClassOf(Class parent, Class child) {
		if (parent == child)
			return true;
		String key = parent.toString() + child.toString();
		Boolean result = checkHash.get(key);
		if (result == null) {
			result = isSuperClassOfEx(parent, child);
			checkHash.put(key, result);
		}
		return result;
	}

	private static boolean isSuperClassOfEx(Class parent, Class child) {
		Set<Class> parents = parent.getParents();
		boolean result = parents.contains(child);
		if (result)
			return true;
		for (Class iter : parents) {
			result = isSuperClassOfEx(iter, child);
			if (result)
				return true;
		}
		return false;
	}

	static final Map<Type, Set<Type>> SUPERCLASS_HASH = new HashMap<Type, Set<Type>>();
	
}
