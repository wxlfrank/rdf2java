package org.open.generate;

import java.util.ArrayList;
import java.util.List;

import org.epos.rdf.annotation.RDF;

@RDF(namespace = "http://somewhere/else#", local = "Class")
public class Class extends Type {

	private List<Field> fields;

	private List<Field> staticFields;

	private Class superClass = null;

	public Class(Package pack, String name) {
		setName(name);
		setContainer(pack);
		if (pack != null)
			pack.addClass(this);
	}

	public void addField(Field field) {
		getFields().add(field);
	}

	public void addStaticField(Field field) {
		field.setContainer(this);
		getStaticFields().add(field);
	}

	public List<Field> getFields() {
		if (fields == null)
			return fields = new ArrayList<Field>();
		return fields;
	}

	public List<Field> getStaticFields() {
		if (staticFields == null)
			return staticFields = new ArrayList<Field>();
		return staticFields;
	}

	public Class getSuperClass() {
		return superClass;
	}

	public void setSuperClass(Class class1) {
		this.superClass = class1;
	}

	List<Interface> _interface = null;;

	public void addInterface(Interface result) {
		get_interface().add(result);
	}

	public List<Interface> get_interface() {
		if (_interface == null)
			_interface = new ArrayList<Interface>();
		return _interface;
	}
}
