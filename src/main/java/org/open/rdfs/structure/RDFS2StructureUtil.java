package org.open.rdfs.structure;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.open.structure.Type;

public class RDFS2StructureUtil {

	private static Set<String> warnings = new LinkedHashSet<String>();

	public static void addWarning(String string) {
		if (warnings.contains(string))
			return;
		warnings.add(string);
	}

	public static void printWarning() {
		List<String> list = new ArrayList<String>(warnings.size());
		list.addAll(warnings);
		list.sort(null);
		list.forEach(s -> System.out.println(s));
	}

	public static Set<Type> getTypes(Set<? extends TypeEx> classexes) {
		Set<Type> result = new LinkedHashSet<Type>();
		for (TypeEx iter : classexes) {
			result.add(iter.getType());
		}
		return result;
	}
}
