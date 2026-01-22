package com.jsmacrosce.doclet.core.model;

import java.util.List;

public record TypeRef(
    TypeKind kind,
    String name,
    String qualifiedName,
    List<TypeRef> typeArgs,
    boolean array,
    boolean varArgs,
    TypeRef bounds,
    boolean nullable
) {
    public TypeRef withNullable(boolean nullable) {
        if (this.nullable == nullable) {
            return this;
        }
        return new TypeRef(kind, name, qualifiedName, typeArgs, array, varArgs, bounds, nullable);
    }
}
