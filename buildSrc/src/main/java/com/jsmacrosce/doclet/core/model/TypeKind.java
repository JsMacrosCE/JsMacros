package com.jsmacrosce.doclet.core.model;

import com.jsmacrosce.doclet.DocletIgnore;

@DocletIgnore
public enum TypeKind {
    PRIMITIVE,
    DECLARED,
    ARRAY,
    TYPEVAR,
    WILDCARD,
    INTERSECTION,
    UNION,
    VOID,
    UNKNOWN
}
