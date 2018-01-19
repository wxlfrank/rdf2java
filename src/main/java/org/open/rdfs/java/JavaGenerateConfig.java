package org.open.rdfs.java;

import org.open.Configuration;

public class JavaGenerateConfig extends Configuration{
	public String getFieldName(String local) {
		local = local.replaceAll("-", "_");
		if (local.equals("abstract") || local.equals("implements") || local.matches("[0-9].*"))
			return "_" + local;
		return local;
	}
}
