package org.open.generate;

import java.util.ArrayList;
import java.util.List;

public class Package {

	private List<Class> classes;
	private List<DataType> datatypes;
	private List<Interface> intfc;

	private String name = null;

	public Package(String packageName) {
		this.name = packageName;
	}

	public void addClass(Class cls) {
		getClasses().add(cls);
		cls.setContainer(this);
	}

	public List<Class> getClasses() {
		if (classes == null) {
			classes = new ArrayList<Class>();
		}
		return classes;
	}

	public List<DataType> getDatatypes() {
		if(datatypes == null)
			datatypes = new ArrayList<DataType>();
		return datatypes;
	}

	public String getName() {
		return name;
	}

	public void setDatatypes(List<DataType> datatypes) {
		this.datatypes = datatypes;
	}

	public void setName(String uri) {
		name = uri;
	}

	public void addInterface(Interface interface1) {
		getInterface().add(interface1);
	}

	public boolean isEmpty() {
		return getInterface().isEmpty();
	}

	public List<Interface> getInterface() {
		if(intfc == null)
			intfc = new ArrayList<Interface>();
		return intfc;
	}
}
