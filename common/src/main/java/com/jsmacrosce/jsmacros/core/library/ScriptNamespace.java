package com.jsmacrosce.jsmacros.core.library;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Gives scripts short names for Java types, such as {@code EntityHelpers.Horse},
 * so scripts do not need to know where those classes live in the Java source.
 * Moving a Java class does not have to break scripts that use its short name.
 * <p>
 * Apply this to a class that lists the types to expose, with a public static final
 * field holding each Java class. The annotation names the group, and each field
 * names one type within it. Register that class with {@link ScriptTypeRegistry}
 * to make the group available to scripts.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ScriptNamespace {
    /** @return the name scripts use for the group, such as {@code EntityHelpers} */
    String value();
}
