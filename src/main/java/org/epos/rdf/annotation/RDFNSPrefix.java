package org.epos.rdf.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = { ElementType.PACKAGE })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(value = RDFNSPrefixes.class)
public @interface RDFNSPrefix {

	String namespace();
	String prefix();
}
