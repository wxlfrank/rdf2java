package org.open.generate;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Resource;

public class DataTypeHash implements TypeHash {

	private Map<Resource, String> datatypes = new HashMap<Resource, String>();
	private RDFS2JavaConfiguration pack;
	public RDFS2JavaConfiguration getPack() {
		return pack;
	}
	public void setPack(RDFS2JavaConfiguration pack) {
		this.pack = pack;
	}
}
