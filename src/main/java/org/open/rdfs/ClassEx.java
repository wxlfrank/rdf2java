package org.open.rdfs;

import org.apache.jena.rdf.model.Resource;

public class ClassEx extends TypeEx {

	private ClassEx superClassEx;

	public ClassEx(PackageEx parent, Resource resource, Class cls) {
		super(parent, resource, cls);
	}

	/**
	 * @param source
	 * @return a newly created field for the resource
	 */
	public FieldEx createField(String local, Resource source) {
		Field field = new Field(get_Class(), local);
		return new FieldEx(this, source, field);
	}

	public Class get_Class() {
		return (Class) getType();
	}

	public FieldEx getFieldEx(String local) {
		return (FieldEx) getChildren().get(local);
	}

	@Override
	public String getHashString() {
		return get_Class().getName();
	}

	public PackageEx getParent() {
		return (PackageEx) parent;
	}

	public void setSuperClass(ClassEx superClassEx) {
		if (this.superClassEx != superClassEx) {
			this.superClassEx = superClassEx;
			if (superClassEx != null)
				this.get_Class().setSuperClass(superClassEx.get_Class());
		}
	}

	public FieldEx toFieldEx(String localName, Resource resource) {
		FieldEx field = getFieldEx(localName);
		if (field == null) {
			field = createField(localName, resource);
		}
		return field;
	}
}
