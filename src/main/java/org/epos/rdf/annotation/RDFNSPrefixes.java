package org.epos.rdf.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = { ElementType.PACKAGE })
@Retention(RetentionPolicy.RUNTIME)
public @interface RDFNSPrefixes {

	RDFNSPrefix[] value();
}
