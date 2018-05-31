package org.open.rdfs.structure;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.jena.rdf.model.Resource;
import org.open.structure.Field;

/**
 * The binding class to bind a resource and the corresponding field in a class
 * 
 * @author wxlfrank
 *
 */
public class FieldEx extends Binding {

	private Set<TypeEx> types = null;

	public FieldEx(ClassEx parent, String name, Resource source) {
		super(parent, source, new Field(parent == null ? null : parent.getClazz(), name));
	}

	public ClassEx getClassEx() {
		return (ClassEx) container;
	}

	public Field getField() {
		return (Field) getTarget();
	}

	@Override
	public String getHash() {
		return getField().getName();
	}

	public Set<TypeEx> getTypes() {
		if (types == null)
			types = new LinkedHashSet<TypeEx>();
		return types;
	}

	public void setTypes(Set<TypeEx> typeEx) {
		this.types = typeEx;
	}

	// public void setType(ClassEx type) {
	// if (type != null) {
	// Set<TypeEx> types = getTypes();
	// if (!types.isEmpty())
	// types.clear();
	// types.add(type);
	// getField().setType(type.get_Class());
	// }
	// }

	public void addType(TypeEx type) {
		getTypes().add(type);
		getField().getTypes().add(type.getType());
	}

	

}
