package org.open;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Util {
	public static <T, U> Set<U> putIntoCache(Map<T, Set<U>> map, T key, U value) {
		Set<U> result = map.get(key);
		if (result == null) {
			result = new LinkedHashSet<U>();
			map.put(key, result);
		}
		result.add(value);
		return result;
	}

	public static <T, U> Set<U> getFromCache(T entity, Map<T, Set<U>> map) {
		Set<U> result = map.get(entity);
		if (result != null)
			return result;
		Set<U> find = new LinkedHashSet<U>();
		map.put(entity, find);
		return find;
	}
	
	public static <T, U, V> V getFromCache(Map<T, Map<U, V>> maps, T key1, U key2) {
		Map<U, V> firstValue = maps.get(key1);
		if (firstValue == null) {
			firstValue = new HashMap<>();
			maps.put(key1, firstValue);
		}
		return firstValue.get(key2);
	}

	public static <T, U, V> void putIntoCache(Map<T, Map<U, V>> maps, T key1, U key2, V value) {
		Map<U, V> firstValue = maps.get(key1);
		if (firstValue == null) {
			firstValue = new HashMap<>();
			maps.put(key1, firstValue);
		}
		firstValue.put(key2, value);
	}
}
