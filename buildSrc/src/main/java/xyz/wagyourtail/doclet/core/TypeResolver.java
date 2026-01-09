package xyz.wagyourtail.doclet.core;

import xyz.wagyourtail.doclet.core.model.TypeRef;

import javax.lang.model.type.TypeMirror;

public interface TypeResolver {
    TypeRef resolve(TypeMirror type);
    String format(TypeRef type, TargetLanguage target);

    default String format(TypeRef type, TargetLanguage target, boolean paramType) {
        return format(type, target);
    }
}
