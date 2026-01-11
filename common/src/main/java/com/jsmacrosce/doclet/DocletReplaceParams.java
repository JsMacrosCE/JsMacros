package com.jsmacrosce.doclet;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.CONSTRUCTOR})
public @interface DocletReplaceParams {
    String value();

}
