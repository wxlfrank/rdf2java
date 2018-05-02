package org.open.rdfs.structure;

import java.util.LinkedHashSet;
import java.util.Set;

import org.open.structure.Type;

public class RDFS2StructureUtil {

	public static Set<Type> getTypes(Set<? extends TypeEx> classexes) {
		Set<Type> result = new LinkedHashSet<Type>();
		for (TypeEx iter : classexes) {
			result.add(iter.getType());
		}
		return result;
	}
}
