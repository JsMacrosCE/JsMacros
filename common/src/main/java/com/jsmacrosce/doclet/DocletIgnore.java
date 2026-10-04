package com.jsmacrosce.doclet;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Documented
@Retention(RetentionPolicy.SOURCE)
@DocletIgnore
public @interface DocletIgnore {
}
