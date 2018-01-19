package org.open.rdfs;

import java.util.HashSet;
import java.util.Set;

import org.apache.jena.rdf.model.Resource;

/**
 * The binding class to bind a resource and the corresponding field in a class
 * 
 * @author wxlfrank
 *
 */
public class FieldEx extends Binding {

	private Set<TypeEx> types = null;

	public FieldEx(ClassEx parent, Resource source, Field field) {
		super(parent, source, field);
	}

	public ClassEx getClassEx() {
		return (ClassEx) parent;
	}

	public Field getField() {
		return (Field) getTarget();
	}

	@Override
	public String getHashString() {
		return getField().getName();
	}

	public Set<TypeEx> getTypes() {
		if (types == null)
			types = new HashSet<TypeEx>();
		return types;
	}

	public void setType(Set<TypeEx> typeEx, Type type) {
		this.types = typeEx;
		if (type != null)
			getField().setType(type);
	}

	public void setTypeEx(ClassEx class1) {
		if (class1 != null) {
			Set<TypeEx> types = getTypes();
			if (!types.isEmpty())
				types.clear();
			types.add(class1);
			getField().setType(class1.get_Class());
		}

	}

}
