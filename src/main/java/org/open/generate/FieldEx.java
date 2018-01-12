package org.open.generate;

import java.util.List;

import org.apache.jena.rdf.model.Resource;

/**
 * The binding class to bind a resource and the corresponding field in a class
 * @author wxlfrank
 *
 */
public class FieldEx {

	public FieldEx(ClassEx parent, Field result, Resource resource) {
		this.parent = parent;
		this.setField(result);
		this.setSource(resource);
	}

	public Field getField() {
		return field;
	}

	public void setField(Field field) {
		this.field = field;
	}

	public Resource getSource() {
		return source;
	}

	public void setSource(Resource source) {
		this.source = source;
	}

	private Field field;
	private Resource source;

	private ClassEx parent = null;

	public ClassEx getClassEx() {
		return parent;
	}

	public List<TypeEx> getType() {
		return type;
	}

	public void setType(List<TypeEx> typeEx, Type type) {
		this.type = typeEx;
		if (type != null)
			field.setType(type);
	}

	private List<TypeEx> type = null;

	// public String toString() {
	// String result = source.getURI();
	// if(result == null)
	// result = source.getId().toString();
	// result += " -> Field " + field.getName();
	// result += ":" + parent.toString() + "->" + String.join(" ", Collections.)
	// }
}
