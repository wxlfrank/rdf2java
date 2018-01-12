package org.epos.tranform;

public class TransformUtils {

	// private static final int _0x20 = 0x20;
	private static final int __0x20 = ~(0x20);
	private static final String VARIABLE_NAME_PATTERN = "[_a-zA-Z][a-zA-Z_0-9]*";

	public static String getName(String name) {
		if (name == null || name.isEmpty() || !name.matches(VARIABLE_NAME_PATTERN))
			return "";
		return (char) (name.charAt(0) & __0x20) + name.substring(1);
	}
}
