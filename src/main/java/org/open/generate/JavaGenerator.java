package org.open.generate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.epos.tranform.TransformUtils;

public class JavaGenerator {
	private RDFS2JavaConfiguration configuration = null;

	public JavaGenerator(RDFS2JavaConfiguration configuration) {
		this.configuration = configuration;
	}

	private static final String IMPORT_FORMAT = "import %s;%n";
	private static final String JAVA_FILE_FORMAT = "%s.java";
	private static final String PACKAGE_HEADER = String.format(JAVA_FILE_FORMAT, "package-info");
	private static final String PACKAGE_FORMAT = "package %s;%n";

	private String toPackageHeader(String name) {
		if (name.isEmpty())
			return name;
		return String.format(PACKAGE_FORMAT, name);
	}

	private String addImport(Type type) {
		if (type == null)
			return "";
		String str = RDFSUtils.getPackageName(type.getContainer().getName());
		if (!str.isEmpty())
			str += ".";
		str += type.getName();
		return String.format(IMPORT_FORMAT, str);
	}

	private void write2File(File pac_dir, String packageInfo, String string) {
		FileWriter writer;
		try {
			writer = new FileWriter(new File(pac_dir, packageInfo));
			writer.write(string);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private final static String CLASS_END = String.format("}%n");
	private final static String CLASS_EXTENDS = " extends %s";
	private final static String CLASS_IMPLEMENTS = " implements %s";
	private final static String CLASS_HEADER = "public class %s";
	private final static String CLASS_START = String.format("{%n");

	private Map<Type, String> getTypeName(List<Type> importedTypes, Map<String, Set<Type>> sameNamedType) {
		Map<Type, String> result = new HashMap<Type, String>();
		importedTypes.forEach(type -> {
			if (!result.containsKey(type)) {
				result.put(type, type.getName());
			}
		});
		sameNamedType.forEach((name, types) -> {
			types.forEach(type -> {
				if (!result.containsKey(type)) {
					if (type.getContainer().getName().equals(Constants.XMLSchema_URI)) {
						switch (type.getName()) {
						case "interger":
							result.put(type, "int");
							break;
						case "string":
							result.put(type, "String");
							break;
						}
					} else
						result.put(type,
								RDFSUtils.getPackageName(type.getContainer().getName()) + "." + type.getName());
				}
			});
		});
		return result;
	}

	/**
	 * 
	 * @param cls
	 *            the class
	 * @return all the types involving in the definition of the class
	 */
	private Map<String, Set<Type>> getDependentTypes(Class cls) {
		Map<String, Set<Type>> sameNamedType = new HashMap<String, Set<Type>>();
		Class superClass = cls.getSuperClass();
		// System.out.println("********************");
		// System.out.println(RDFUtils.getPackageName(cls.getContainer().getName()) +
		// "." + cls.getName());
		if (superClass != null)
			Utils.putIntoMap(sameNamedType, superClass.getName(), superClass);
		cls.getInterface().forEach(intfc -> Utils.putIntoMap(sameNamedType, intfc.getName(), intfc));
		cls.getFields().forEach(field -> {
			Type type = field.getType();
			if (type != null)
				Utils.putIntoMap(sameNamedType, type.getName(), type);
		});
		return sameNamedType;
	}

	private List<Type> getImportedTypes(Class cls, Map<String, Set<Type>> dependentTypes) {
		String class_name = cls.getName();
		List<Type> result = new ArrayList<Type>();
		dependentTypes.forEach((name, types) -> {
			if (!class_name.equals(name)) {
				Type main = types.iterator().next();
				if (!main.getContainer().getName().equals(Constants.XMLSchema_URI)) {
					types.remove(main);
					result.add(main);
				}
			}
		});
		return result;
	}

	protected String toJavaClassBody(Class cls, Map<Type, String> type2name) {
		// System.out.println("________________");
		// System.out.println(RDFUtils.getPackageName(cls.getContainer().getName()) +
		// "." + cls.getName());
		// type2name.forEach((type, str) -> {
		// System.out.println(type.getName() + str);
		// });
		StringBuffer buffer = new StringBuffer();
		buffer.append(String.format(CLASS_HEADER, cls.getName()));
		Class superClass = cls.getSuperClass();
		if (superClass != null)
			buffer.append(String.format(CLASS_EXTENDS, type2name.get(superClass)));
		List<String> intfcs = new ArrayList<String>();
		cls.getInterface().forEach(intfc -> intfcs.add(type2name.get(intfc)));
		if (!intfcs.isEmpty())
			buffer.append(String.format(CLASS_IMPLEMENTS, String.join(", ", intfcs)));
		buffer.append(CLASS_START);
		cls.getStaticFields().forEach(field -> {
			// System.out.println(field.getType());
			buffer.append("public static final " + field.getType().getName() + " " + field.getName() + " = new "
					+ type2name.get(field.getType()) + "();");
			buffer.append(System.lineSeparator());
		});
		cls.getFields().forEach(field -> buffer.append(toJavaFieldStatement(type2name, field)));
		buffer.append(CLASS_END);
		return buffer.toString();
	}

	private static final String FIELD_FORMAT = "\tprivate %1$s %2$s;%n" + "\tpublic %1$s get%3$s() {%n"
			+ "\t\treturn %2$s;%n" + "\t}%n" + "\tpublic void set%3$s(%1$s %2$s) {%n" + "\t\tthis.%2$s = %2$s;%n"
			+ "\t}%n";

	protected String toJavaFieldStatement(Map<Type, String> type2name, Field field) {
		Type type = field.getType();
		String name = configuration.getFieldName(field.getName());
		// System.out.println("************************" + field.getType());
		// System.out.println(name + "->" + TransformUtils.getName(name));
		return String.format(FIELD_FORMAT, type == null ? null : type2name.get(type), name,
				TransformUtils.getName(name));

	}

	public void generate(File location) {
		if (!location.isDirectory() && configuration == null)
			return;
		if (!location.exists()) {
			location.mkdirs();
		}
		configuration.getHash().forEach((ns, packex) -> {
			Package pac = packex.getPackage();
			if (packex != Constants.XMLSchema_Package) {
				String pacName = RDFSUtils.getPackageName(pac.getName());
				File pac_dir = new File(location, String.join(File.separator, pacName.split("\\.")));
				if (!pac_dir.exists()) {
					pac_dir.mkdirs();
				}
				String package_header = toPackageHeader(pacName);
				write2File(pac_dir, PACKAGE_HEADER, package_header);
				pac.getClasses().forEach(cls -> {
					StringBuffer buffer = new StringBuffer();
					buffer.append(package_header);
					Map<String, Set<Type>> dependentTypes = getDependentTypes(cls);
					List<Type> importedTypes = getImportedTypes(cls, dependentTypes);
					importedTypes.forEach(importType -> {
						buffer.append(addImport(importType));
					});
					write2File(pac_dir, String.format(JAVA_FILE_FORMAT, cls.getName()),
							buffer.toString() + toJavaClassBody(cls, getTypeName(importedTypes, dependentTypes)));
				});
				pac.getInterface().forEach(intf -> {
					StringBuffer buffer = new StringBuffer();
					buffer.append(package_header);
					write2File(pac_dir, String.format(JAVA_FILE_FORMAT, intf.getName()),
							buffer.toString() + intf.toString());
				});
			}
		});
	}
}
