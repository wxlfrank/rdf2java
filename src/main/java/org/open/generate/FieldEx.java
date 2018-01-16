package org.open.generate;

import java.util.Set;

import org.apache.jena.rdf.model.Resource;

/**
 * The binding class to bind a resource and the corresponding field in a class
 * 
 * @author wxlfrank
 *
 */
public class FieldEx extends Binding {

	public FieldEx(ClassEx parent, Resource source, Field field) {
		super(parent, source, field);
	}

	public Field getField() {
		return (Field) getTarget();
	}

	public ClassEx getClassEx() {
		return (ClassEx) parent;
	}

	public Set<TypeEx> getTypes() {
		return types;
	}

	public void setType(Set<TypeEx> typeEx, Type type) {
		this.types = typeEx;
		if (type != null)
			getField().setType(type);
	}

	private Set<TypeEx> types = null;

	@Override
	public String getHashString() {
		return getField().getName();
	}

}
