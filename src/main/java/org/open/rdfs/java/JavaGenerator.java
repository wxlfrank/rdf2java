package org.open.rdfs.java;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.open.rdfs.Binding;
import org.open.rdfs.Class;
import org.open.rdfs.Constants;
import org.open.rdfs.Field;
import org.open.rdfs.Package;
import org.open.rdfs.PackageEx;
import org.open.rdfs.Type;
import org.open.rdfs.Utils;

public class JavaGenerator {
	public static final String RDF_ANNOTATION_IMPORT = String
			.format("import " + org.open.rdf.annotation.RDF.class.getName() + ";%n");
	private final static String CLASS_END_BRACKET = String.format("}%n");

	private final static String CLASS_EXTENDS = " extends %s";

	private final static String CLASS_HEADER = "public class %s";
	private final static String CLASS_IMPLEMENTS = " implements %s";
	private final static String CLASS_RDF_ANNOTATION = "@RDF(namespace = \"%1$s\", local = \"%2$s\")%n";
	private final static String CLASS_START_BRACKET = String.format("{%n");

	private static final String FIELD_DECLARE = "\tprivate %1$s %2$s;%n";
	private static final String FIELD_GETTER = "\tpublic %1$s get%3$s() {%n" + "\t\treturn %2$s;%n" + "\t}%n";
	private static final String FIELD_RDF_ANNOTATION = "\t@RDF(namespace = \"%4$s\", local = \"%5$s\")%n";
	private static final String FIELD_SETTER = "\tpublic void set%3$s(%1$s %2$s) {%n" + "\t\tthis.%2$s = %2$s;%n"
			+ "\t}%n";
	private static final String FIELD_FORMAT = FIELD_RDF_ANNOTATION + FIELD_DECLARE + FIELD_GETTER + FIELD_SETTER;
	private static final String COLLECTION_FIELD_FORMAT = FIELD_RDF_ANNOTATION + "\tprivate List<%1$s> %2$s;%n"
			+ "\tpublic List<%1$s> get%3$s() {%n" + "\t\tif(%2$s == null) {%n" + "\t\t\t%2$s = new ArrayList<%1$s>();%n"
			+ "\t\t}%n" + "\t\treturn %2$s;%n" + "\t}%n";

	private static final String IMPORT_FORMAT = "import %s;%n";
	private static final String JAVA_FILE_FORMAT = "%s.java";
	private static final String PACKAGE_FORMAT = "package %s;%n";
	private static final String PACKAGE_HEADER = String.format(JAVA_FILE_FORMAT, "package-info");
	private static final String PACKAGE_RDF_ANNOTATION = "@RDF(namespace = \"%1$s\")%n";

	private JavaGenerateConfig config = null;

	public JavaGenerator(JavaGenerateConfig config) {
		this.config = config;
	}

	/**
	 * transform a package into a java package and visit included classes and
	 * transform them corresponding java classes.
	 * 
	 * @param packex
	 * @param location
	 */
	protected void visit(PackageEx packex, File location) {
		Package pac = packex.getPackage();
		if (packex != Constants.XMLSchema_Package) {
			String pacName = config.getPackageName(pac.getName());
			File pac_dir = new File(location, String.join(File.separator, pacName.split("\\.")));
			if (!pac_dir.exists()) {
				pac_dir.mkdirs();
			}
			String packageStatement = toPackageStatement(pacName);
			String packageFileContent = String.format(PACKAGE_RDF_ANNOTATION, pac.getName()) + packageStatement
					+ RDF_ANNOTATION_IMPORT;
			write2File(pac_dir, PACKAGE_HEADER, packageFileContent);
			for (Class cls : pac.getClasses()) {
				visit(cls, pac_dir, packageStatement);
			}
			pac.getInterface().forEach(intf -> {
				StringBuffer buffer = new StringBuffer();
				buffer.append(packageStatement);
				write2File(pac_dir, String.format(JAVA_FILE_FORMAT, intf.getName()),
						buffer.toString() + intf.toString());
			});
		}

	}

	/**
	 * Transform a class into java class and transform included fields
	 * 
	 * @param cls
	 * @param dir
	 * @param package_header
	 */
	protected void visit(Class cls, File dir, String package_header) {
		StringBuffer classBuffer = new StringBuffer();
		classBuffer.append(package_header);
		Map<String, Set<Type>> dependentTypes = getDependentTypes(cls);
		List<Type> importedTypes = getImportedTypes(cls, dependentTypes);
		importedTypes.forEach(importType -> {
			classBuffer.append(addImport(importType));
		});
		classBuffer.append(RDF_ANNOTATION_IMPORT);
		Map<Type, String> local_type_dic = getLocalTypeName(cls, importedTypes, dependentTypes);
		visit(cls, classBuffer, local_type_dic);
		write2File(dir, String.format(JAVA_FILE_FORMAT, cls.getName()), classBuffer.toString());
	}

	public void generate(File location, Collection<Binding> packages) {
		if (!location.isDirectory() && config == null)
			return;
		if (!location.exists()) {
			location.mkdirs();
		}
		for (Binding bind : packages) {
			visit((PackageEx) bind, location);
		}
	}

	/**
	 * Get the import statement for the type
	 * 
	 * @param type
	 * @return
	 */
	private String addImport(Type type) {
		if (type == null)
			return "";

		String full_name = "";
		if (type instanceof JavaType) {
			full_name = type.getName();
		} else {
			full_name = config.getPackageName(type.getContainer().getName());
			if (!full_name.isEmpty())
				full_name += ".";
			full_name += type.getName();
		}
		return String.format(IMPORT_FORMAT, full_name);
	}

	/**
	 * Retrieve all the types shown in the Class cls
	 * 
	 * @param cls
	 *            the class
	 * @return all the types involving in the definition of the class hashed by
	 *         the names of classes
	 */
	private Map<String, Set<Type>> getDependentTypes(Class cls) {
		Map<String, Set<Type>> sameNamedType = new HashMap<String, Set<Type>>();
		Class superClass = cls.getSuperClass();
		if (superClass != null)
			Utils.putIntoMap(sameNamedType, superClass.getName(), superClass);
		cls.get_interface().forEach(intfc -> Utils.putIntoMap(sameNamedType, intfc.getName(), intfc));
		boolean needList = false;
		for (Field field : cls.getFields()) {
			Type type = field.getType();
			if (field.getMultiplicity() > 1)
				needList = true;
			if (type != null)
				Utils.putIntoMap(sameNamedType, type.getName(), type);
		}
		if (needList) {
			Utils.putIntoMap(sameNamedType, JavaType.LIST.getName(), JavaType.LIST);
			Utils.putIntoMap(sameNamedType, JavaType.ARRAYLIST.getName(), JavaType.ARRAYLIST);
		}
		return sameNamedType;
	}

	/**
	 * Get all the types that should be imported in the java file
	 * 
	 * @param cls
	 * @param dependentTypes
	 * @return
	 */

	private List<Type> getImportedTypes(Class cls, Map<String, Set<Type>> dependentTypes) {
		String class_name = cls.getName();
		List<Type> result = new ArrayList<Type>();
		dependentTypes.forEach((name, types) -> {
			if (!class_name.equals(name)) {
				Type main = types.iterator().next();
				if (main instanceof JavaType || !main.getContainer().getName().equals(Constants.XMLSchema_URI)) {
					types.remove(main);
					result.add(main);
				}
			}
		});
		return result;
	}

	/**
	 * Get the type name in a java class file whether its full name for
	 * unimported class or simple name for imported class
	 * 
	 * @param importedTypes
	 *            imported types
	 * @param sameNamedType
	 *            type hashed by simple name
	 * @return
	 */
	private Map<Type, String> getLocalTypeName(Class cls, List<Type> importedTypes, Map<String, Set<Type>> sameNamedType) {
		Map<Type, String> result = new HashMap<Type, String>();
		result.put(cls, cls.getName());
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
						result.put(type, config.getPackageName(type.getContainer().getName()) + "." + type.getName());
				}
			});
		});
		return result;
	}

	/**
	 * generate package statement given a valid java package name
	 * 
	 * @param name
	 * @return
	 */
	private String toPackageStatement(String name) {
		if (name.isEmpty())
			return name;
		return String.format(PACKAGE_FORMAT, name);
	}

	/**
	 * write content to a file in the directory dir
	 * 
	 * @param dir
	 * @param file
	 * @param content
	 */
	private void write2File(File dir, String file, String content) {
		FileWriter writer;
		try {
			writer = new FileWriter(new File(dir, file));
			writer.write(content);
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
	 *            the hash table that maps the type of a field to its generated
	 *            name
	 * @return
	 */
	protected void visit(Class cls, StringBuffer classBuffer, Map<Type, String> type2name) {
		classBuffer.append(String.format(CLASS_RDF_ANNOTATION, cls.getContainer().getName(), cls.getName()));
		classBuffer.append(String.format(CLASS_HEADER, cls.getName()));
		Class superClass = cls.getSuperClass();
		if (superClass != null)
			classBuffer.append(String.format(CLASS_EXTENDS, type2name.get(superClass)));
		List<String> intfcs = new ArrayList<String>();
		cls.get_interface().forEach(intfc -> intfcs.add(type2name.get(intfc)));
		if (!intfcs.isEmpty())
			classBuffer.append(String.format(CLASS_IMPLEMENTS, String.join(", ", intfcs)));
		classBuffer.append(CLASS_START_BRACKET);
		for (Field field : cls.getFields()) {
			visit(field, classBuffer, type2name);
		}
		classBuffer.append(CLASS_END_BRACKET);
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
	private void visit(Field field, StringBuffer classBuffer, Map<Type, String> type2name) {
		Type type = field.getType();
		String name = config.getFieldName(field.getName());
		Package pack = field.getContainer().getContainer();
		classBuffer.append(toJavaField(field.getMultiplicity() > 1 ? COLLECTION_FIELD_FORMAT : FIELD_FORMAT,
				type == null ? null : type2name.get(type), name, Utils.captializeFirstChar(name), pack.getName(),
				field.getName()));

	}

	/**
	 * generate java field definition
	 * 
	 * @param format
	 * @param type
	 * @param fieldName
	 * @param methodName
	 * @param rdfPackage
	 * @param rdfPart
	 * @return
	 */
	private String toJavaField(String format, String type, String fieldName, String methodName, String rdfPackage,
			String rdfPart) {
		return String.format(format, type, fieldName, methodName, rdfPackage, rdfPart);
	}

}
