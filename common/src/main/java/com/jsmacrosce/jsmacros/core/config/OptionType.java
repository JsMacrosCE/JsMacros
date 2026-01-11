package com.jsmacrosce.jsmacros.core.config;

public @interface OptionType {
    String value() default "primitive";

    String[] options() default {};

}
