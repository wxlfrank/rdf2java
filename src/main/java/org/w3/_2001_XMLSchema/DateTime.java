package org.w3._2001_XMLSchema;

import org.open.rdf.annotation.RDFLiteral;

@RDFLiteral(namespace = "http://www.w3.org/2001/XMLSchema#", local = "dateTime")
public class DateTime {
	private Object value;

	public DateTime(Object value) {
		this.value = value;
	}

	public java.lang.String toString() {
		return value != null ? value.toString() : "null";
	}
}
