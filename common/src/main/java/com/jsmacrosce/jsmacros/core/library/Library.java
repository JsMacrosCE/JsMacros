package com.jsmacrosce.jsmacros.core.library;

import com.jsmacrosce.jsmacros.core.language.BaseLanguage;

import java.lang.annotation.*;

/**
 * Marks a library class and supplies its script-global name and optional guest-language restrictions.
 * For shared and per-execution libraries, an empty language list allows all languages. Per-language
 * library types are registered only for the listed language classes.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Library {
    /** @return the script-global name for this library */
    String value();

    /** @return guest-language classes for this library; empty allows all only for shared and per-execution library types */
    Class<? extends BaseLanguage<?, ?>>[] languages() default {};

}
