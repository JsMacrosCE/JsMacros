package com.jsmacrosce.doclet;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
@DocletIgnore
public @interface DocletDeclareType {
    String name();
    String type();
}
