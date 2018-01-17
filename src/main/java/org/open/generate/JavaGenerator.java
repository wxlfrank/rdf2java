package org.open.generate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.epos.tranform.RDFS2Java;
import org.epos.tranform.TransformUtils;

public class JavaGenerator {
	public static final String RDF_ANNOTATION_IMPORT = String.format("import org.epos.rdf.annotation.RDF;%n");
	private final static String CLASS_END = String.format("}%n");

	private final static String CLASS_EXTENDS = " extends %s";

	private final static String CLASS_HEADER = "public class %s";
	private final static String CLASS_IMPLEMENTS = " implements %s";
	private final static String CLASS_RDF_ANNOTATION = "@RDF(namespace = \"%1$s\", local = \"%2$s\")%n";
	private final static String CLASS_START = String.format("{%n");

	private static final String FIELD_DECLARE = "\tprivate %1$s %2$s;%n";


	private static final String FIELD_GETTER = "\tpublic %1$s get%3$s() {%n" + "\t\treturn %2$s;%n" + "\t}%n";

	private static final String FIELD_RDF_ANNOTATION = "\t@RDF(namespace = \"%4$s\", local = \"%5$s\")%n";
	private static final String FIELD_SETTER = "\tpublic void set%3$s(%1$s %2$s) {%n" + "\t\tthis.%2$s = %2$s;%n" + "\t}%n";
	private static final String FIELD_FORMAT = FIELD_RDF_ANNOTATION + FIELD_DECLARE + FIELD_GETTER + FIELD_SETTER;
	private static final String IMPORT_FORMAT = "import %s;%n";
	private static final String JAVA_FILE_FORMAT = "%s.java";
	private static final String PACKAGE_FORMAT = "package %s;%n";
	private static final String PACKAGE_HEADER = String.format(JAVA_FILE_FORMAT, "package-info");

	private RDFS2JavaConfiguration configuration = null;

	private RDFS2Java rdfs2Java = null;

	public JavaGenerator(RDFS2Java rdfs2Java, RDFS2JavaConfiguration configuration2) {
		this.configuration = configuration2;
		this.rdfs2Java = rdfs2Java;
	}

	public void generate(File location) {
		if (!location.isDirectory() && configuration == null)
			return;
		if (!location.exists()) {
			location.mkdirs();
		}
		rdfs2Java.getChildren().forEach((ns, bind) -> {
			PackageEx packex = (PackageEx) bind;
			Package pac = packex.getPackage();
			if (packex != Constants.XMLSchema_Package) {
				String pacName = RDFSUtils.getPackageName(pac.getName());
				File pac_dir = new File(location, String.join(File.separator, pacName.split("\\.")));
				if (!pac_dir.exists()) {
					pac_dir.mkdirs();
				}
				String package_header = toPackageHeader(pacName);
				write2File(pac_dir, PACKAGE_HEADER, package_header);
				// for(ClassEx clsEx : packex.getClassexes()) {
				// StringBuffer buffer = new StringBuffer();
				// buffer.append(package_header);
				// Map<String, Set<Type>> dependentTypes = getDependentTypes(cls);
				// List<Type> importedTypes = getImportedTypes(cls, dependentTypes);
				// importedTypes.forEach(importType -> {
				// buffer.append(addImport(importType));
				// });
				// buffer.append(RDF_ANNOTATION_IMPORT);
				// write2File(pac_dir, String.format(JAVA_FILE_FORMAT, cls.getName()),
				// buffer.toString() + toJavaClassBody(cls, getTypeName(importedTypes,
				// dependentTypes)));
				// }
				pac.getClasses().forEach(cls -> {
					StringBuffer buffer = new StringBuffer();
					buffer.append(package_header);
					Map<String, Set<Type>> dependentTypes = getDependentTypes(cls);
					List<Type> importedTypes = getImportedTypes(cls, dependentTypes);
					importedTypes.forEach(importType -> {
						buffer.append(addImport(importType));
					});
					buffer.append(RDF_ANNOTATION_IMPORT);
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

	private String addImport(Type type) {
		if (type == null)
			return "";
		String str = RDFSUtils.getPackageName(type.getContainer().getName());
		if (!str.isEmpty())
			str += ".";
		str += type.getName();
		return String.format(IMPORT_FORMAT, str);
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
		if (superClass != null)
			Utils.putIntoMap(sameNamedType, superClass.getName(), superClass);
		cls.get_interface().forEach(intfc -> Utils.putIntoMap(sameNamedType, intfc.getName(), intfc));
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
						case "integer":
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
	private String toPackageHeader(String name) {
		if (name.isEmpty())
			return name;
		return String.format(PACKAGE_FORMAT, name);
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

	/**
	 * Generate the body of the java class {@code cls}
	 * 
	 * @param cls
	 *            the class to be translated
	 * @param type2name
	 *            the hash table that maps the type of a field to its generated name
	 * @return
	 */
	protected String toJavaClassBody(Class cls, Map<Type, String> type2name) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(String.format(CLASS_RDF_ANNOTATION, cls.getContainer().getName(), cls.getName()));
		buffer.append(String.format(CLASS_HEADER, cls.getName()));
		Class superClass = cls.getSuperClass();
		if (superClass != null)
			buffer.append(String.format(CLASS_EXTENDS, type2name.get(superClass)));
		List<String> intfcs = new ArrayList<String>();
		cls.get_interface().forEach(intfc -> intfcs.add(type2name.get(intfc)));
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

	/**
	 * Generate java fields and their getters and setters respectively
	 * 
	 * @param type2name
	 *            the hash table that maps the type of the {@code field} to its
	 *            generated name
	 * @param field
	 *            the java field
	 * @return the string representing the field
	 */
	protected String toJavaFieldStatement(Map<Type, String> type2name, Field field) {
		Type type = field.getType();
		String name = configuration.getFieldName(field.getName());
		Package pack = field.getContainer().getContainer();
		return String.format(FIELD_FORMAT, type == null ? null : type2name.get(type), name,
				TransformUtils.getName(name), pack.getName(), field.getName());

	}
}
