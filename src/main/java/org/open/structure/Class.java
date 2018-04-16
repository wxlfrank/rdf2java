package org.open.structure;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Class extends Inheritable {

	private List<Field> fields;

	private Set<Class> parents = null;

	public Set<Class> getParents() {
		if (parents == null)
			parents = new LinkedHashSet<Class>();
		return parents;
	}

	public Class(Package pack, String name) {
		setName(name);
		setContainer(pack);
		if (pack != null)
			pack.addClass(this);
	}

	public void addField(Field field) {
		getFields().add(field);
	}

	public List<Field> getFields() {
		if (fields == null)
			return fields = new ArrayList<Field>();
		return fields;
	}

	public void addParent(Class _Class) {
		if (_Class != null)
			getParents().add(_Class);
	};

	public boolean isTypeOf(Type baseType) {
		boolean result = super.isTypeOf(baseType);
		if (result == true)
			return true;
		for (Class parent : getParents()) {
			result = parent.isTypeOf(baseType);
			if (result == true)
				return true;
		}
		return false;
	}
}
