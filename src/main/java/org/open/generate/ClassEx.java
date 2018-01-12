package org.open.generate;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Resource;

public class ClassEx extends TypeEx {

	private Map<String, FieldEx> fields = new HashMap<String, FieldEx>();
	private PackageEx parent;

	String field_prefix = null;

	public ClassEx(PackageEx parent, Class cls, Resource resource) {
		super(cls, resource);
		this.parent = parent;
	}

	/**
	 * @param name
	 * @return the field corresponding to the property name {@code name}
	 */
	public FieldEx getField(String name) {
		// if(configuration != null)
		// local = configuration.getFieldName(local);
		return fields.get(name);
	}

	public Class getType() {
		return (Class) type;
	}

	/**
	 * @param resource
	 * @return a newly created field for the resource
	 */
	public FieldEx createField(Resource resource) {
		String local = resource.getLocalName();
		// if(configuration != null)
		// local = configuration.getFieldName(local);
		FieldEx ex = new FieldEx(this, new Field(getType(), local), resource);
		fields.put(local, ex);
		return ex;
	}

	public PackageEx getParent() {
		return parent;
	}
	// private RDFS2JavaConfiguration configuration;
	// public void setConfiguration(RDFS2JavaConfiguration configuration) {
	// this.configuration = configuration;
	// }

	ClassEx superClassEx;

	public void setSuperClass(ClassEx superClassEx) {
		if (this.superClassEx != superClassEx) {
			this.superClassEx = superClassEx;
			if (superClassEx != null)
				this.getType().setSuperClass(superClassEx.getType());
		}
	}
}
