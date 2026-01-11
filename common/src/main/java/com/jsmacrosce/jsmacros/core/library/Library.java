package com.jsmacrosce.jsmacros.core.library;

import com.jsmacrosce.jsmacros.core.language.BaseLanguage;

import java.lang.annotation.*;

/**
 * Base Function interface.
 *
 * @author Wagyourtail
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Library {
    String value();

    Class<? extends BaseLanguage<?, ?>>[] languages() default {};

}
