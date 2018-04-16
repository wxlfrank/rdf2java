package org.open.rdf.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = { ElementType.TYPE })
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface RDFLiteral {

	String local() default "";

	String namespace() default "";
}
