package org.open.rdf;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.open.rdf.annotation.RDF;
import org.open.rdfs.Utils;

public class JavaReflectUtils {
	private Map<Class<?>, Map<Field, Object>> javaFieldGetter = new HashMap<Class<?>, Map<Field, Object>>();

	private Map<Class<?>, Map<String, Object>> javaFieldSetter = new HashMap<Class<?>, Map<String, Object>>();
	/**
	 * get the filed accessors for the java class
	 * 
	 * @param java_class
	 * @return
	 */
	public Map<Field, Object> getJavaFieldGetter(Class<?> java_class) {
		Map<Field, Object> fieldAccessor = javaFieldGetter.get(java_class);
		if (fieldAccessor == null) {
			fieldAccessor = createJavaFieldGetter(java_class);
			javaFieldGetter.put(java_class, fieldAccessor);
		}
		return fieldAccessor;
	}

	/**
	 * get the filed accessors for the java class
	 * 
	 * @param java_class
	 * @return
	 */
	public Map<String, Object> getJavaFieldSetter(Class<?> java_class) {
		Map<String, Object> fieldAccessor = javaFieldSetter.get(java_class);
		if (fieldAccessor == null) {
			fieldAccessor = createJavaFieldSetter(java_class);
			javaFieldSetter.put(java_class, fieldAccessor);
		}
		return fieldAccessor;
	}

	/**
	 * Get the value of the {@code field} for the {@code java_object} which is of
	 * the type {@code java_class} using the field {@code accessor}
	 * 
	 * @param java_class
	 *            a java class
	 * @param field
	 *            a java field
	 * @param accessor
	 *            the accessor of the the field, i.e., field itself or the method
	 * @param java_object
	 *            the java object
	 * @return the value
	 */
	public Object getJavaFieldValue(Class<?> java_class, Object accessor, Object java_object) {
		if (accessor instanceof Field) {
			try {
				return ((Field) accessor).get(java_object);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		} else if (accessor instanceof Method) {
			try {
				return ((Method) accessor).invoke(java_object);
			} catch (SecurityException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException e) {
				e.printStackTrace();
			}

		}
		return null;
	}

	public void setJavaFieldSetter(Class<? extends Object> class1, Map<String, Object> setter) {
		javaFieldSetter.put(class1, setter);
	}

	public void setProperty(Object accessor, Object obj, Object value) {
		if (accessor instanceof Field) {
			try {
				((Field) accessor).set(obj, value);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		} else if (accessor instanceof Method) {
			try {
				((Method) accessor).invoke(obj, value);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * create field accessor for each field of the {@code java_class}
	 * 
	 * @param java_class
	 * @return the created field accessors
	 */
	protected Map<Field, Object> createJavaFieldGetter(Class<?> java_class) {
		Map<Field, Object> fieldAccessor = new HashMap<Field, Object>();
		for (Field field : java_class.getDeclaredFields()) {
			try {
				fieldAccessor.put(field,
						field.isAccessible() ? field : java_class.getMethod(Utils.getGetName(field.getName())));
			} catch (NoSuchMethodException | SecurityException | NullPointerException | IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return fieldAccessor;
	}

	/**
	 * create field accessor for each field of the {@code java_class}
	 * 
	 * @param java_class
	 * @return the created field accessors
	 */
	protected Map<String, Object> createJavaFieldSetter(Class<?> java_class) {
		Map<String, Object> fieldAccessor = new HashMap<String, Object>();
		for (Field field : java_class.getDeclaredFields()) {
			RDF rdf = field.getAnnotation(RDF.class);
			if (rdf != null) {
				String uri = rdf.namespace() + rdf.local();
				try {
					fieldAccessor.put(uri,
							field.isAccessible() ? field : java_class.getMethod(Utils.getSetName(field.getName()), field.getType()));
				} catch (NoSuchMethodException | SecurityException | NullPointerException | IllegalStateException e) {
					e.printStackTrace();
				}
			}
		}
		return fieldAccessor;
	}
}
