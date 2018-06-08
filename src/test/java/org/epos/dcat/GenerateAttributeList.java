package org.epos.dcat;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.w3.ns_dcat.Dataset;

public class GenerateAttributeList {

	@Test
	public void test() {
		Class<Dataset> cur = Dataset.class;
		List<String> results = new ArrayList<String>();
		for (Field field : cur.getDeclaredFields()) {
			String typeString = field.getType().getSimpleName();
			Type type = field.getGenericType();
			if (type instanceof ParameterizedType) {
				ParameterizedType stringListType = (ParameterizedType) type;
				Class<?> stringListClass = (Class<?>) stringListType.getActualTypeArguments()[0];
				typeString += "<" + stringListClass.getSimpleName() + ">";
			}
			results.add(field.getName() + ':' + typeString);
		}
		Collections.sort(results);
		results.forEach(a -> System.out.println(a));
	}
}
