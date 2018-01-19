package org.open.rdfs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Utils {

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

	public static <T, U, V> V get(Map<T, Map<U, V>> maps, T key1, U key2) {
		Map<U, V> firstValue = maps.get(key1);
		if (firstValue == null) {
			firstValue = new HashMap<>();
			maps.put(key1, firstValue);
		}
		return firstValue.get(key2);
	}

	public static String getGetName(String name) throws NullPointerException, IllegalStateException {
		checkJavaVariable(name);
		return "get" + captializeFirstChar(name);
	}

	public static String getSetName(String name) throws NullPointerException, IllegalStateException {
		checkJavaVariable(name);
		return "set" + captializeFirstChar(name);
	}

	public static <T, U, V> void put(Map<T, Map<U, V>> maps, T key1, U key2, V value) {
		Map<U, V> firstValue = maps.get(key1);
		if (firstValue == null) {
			firstValue = new HashMap<>();
			maps.put(key1, firstValue);
		}
		firstValue.put(key2, value);
	}

	public static <T, U> Set<U> putIntoMap(Map<T, Set<U>> map, T key, U value) {
		Set<U> result = map.get(key);
		if (result == null) {
			result = new HashSet<U>();
			map.put(key, result);
		}
		result.add(value);
		return result;
	}
}
