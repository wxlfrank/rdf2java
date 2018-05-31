package org.open.rdfs.structure;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.jena.rdf.model.Resource;
import org.open.structure.Class;

public class ClassEx extends TypeEx {

	private Set<ClassEx> parents;

	public ClassEx(PackageEx container, String name, Resource resource) {
		super(container, resource, new Class(container.getPackage(), name));
	}

	public Class getClazz() {
		return (Class) getType();
	}

	public FieldEx getFieldEx(String local) {
		FieldEx result = (FieldEx) getContents().get(local);
		if (result != null)
			return result;
		for (ClassEx clazz : getParents()) {
			result = clazz.getFieldEx(local);
			if (result != null)
				return result;
		}
		return null;
	}

	@Override
	public String getHash() {
		return getClazz().getName();
	}

	public PackageEx getContainer() {
		return (PackageEx) container;
	}

	public void addParent(ClassEx parent) {
		if (parent != null) {
			getParents().add(parent);
			getClazz().addParent(parent.getClazz());
		}
	}

	public Set<ClassEx> getParents() {
		if (parents == null)
			parents = new LinkedHashSet<ClassEx>();
		return parents;
	}

	public void setParents(Set<ClassEx> par_Parents) {
		parents = par_Parents;
		Set<Class> temp = getClazz().getParents();
		parents.forEach(parent -> temp.add(parent.getClazz()));
	}

	public Collection<? extends Binding> getFields() {
		return contents.values();
	}
}
