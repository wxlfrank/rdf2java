package org.open.generate;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Utils {

	public static <T, U extends Type> Set<U> putIntoMap(Map<T, Set<U>> map, T key, U value) {
		Set<U> result = map.get(key);
		if (result == null) {
			result = new HashSet<U>();
			map.put(key, result);
		}
		result.add(value);
		return result;
	}
}
