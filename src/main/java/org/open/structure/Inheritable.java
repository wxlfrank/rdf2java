package org.open.structure;

import java.util.HashSet;
import java.util.Set;

public class Inheritable extends Type {
	private Set<Interface> interfaces = null;

	public void addInterface(Interface result) {
		getInterfaces().add(result);
	}

	public Set<Interface> getInterfaces() {
		if (interfaces == null)
			interfaces = new HashSet<Interface>();
		return interfaces;
	}
}
