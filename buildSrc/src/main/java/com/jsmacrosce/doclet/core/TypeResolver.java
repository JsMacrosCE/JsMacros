package com.jsmacrosce.doclet.core;

import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.doclet.core.model.TypeRef;

import javax.lang.model.type.TypeMirror;

@DocletIgnore
public interface TypeResolver {
    TypeRef resolve(TypeMirror type);
    String format(TypeRef type, TargetLanguage target);

    default String format(TypeRef type, TargetLanguage target, boolean paramType) {
        return format(type, target);
    }

    /**
     * Formats a type the definitions build on. Such a type is named by the type it is, not by
     * the alias a script uses for it, because an alias cannot always be built on.
     */
    default String formatHeritage(TypeRef type) {
        return format(type, TargetLanguage.TYPESCRIPT, false);
    }
}
