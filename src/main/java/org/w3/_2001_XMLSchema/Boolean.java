package org.w3._2001_XMLSchema;
import org.open.rdf.annotation.RDFLiteral;
@RDFLiteral(namespace = "http://www.w3.org/2001/XMLSchema#", local = "boolean")
public class Boolean{
	private Object value;
	public Boolean(Object value) {
		this.value = value;
	}
	public java.lang.String toString() {
		return value != null ? value.toString() : "null";
	}
}
