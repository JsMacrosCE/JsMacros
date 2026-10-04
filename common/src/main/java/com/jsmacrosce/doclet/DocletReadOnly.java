package com.jsmacrosce.doclet;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field that scripts should read but not assign. The owning Java code may
 * still update it; this is a documentation and typing contract, not runtime enforcement
 * or a guarantee that the referenced object is immutable.
 */
@DocletIgnore
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface DocletReadOnly {
}
