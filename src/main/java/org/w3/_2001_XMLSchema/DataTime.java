package org.w3._2001_XMLSchema;
import org.open.rdf.annotation.RDFLiteral;
@RDFLiteral(namespace = "http://www.w3.org/2001/XMLSchema#", local = "dataTime")
public class DataTime{
	private Object value;
	public DataTime(Object value) {
		this.value = value;
	}
	public java.lang.String toString() {
		return value != null ? value.toString() : "null";
	}
}
