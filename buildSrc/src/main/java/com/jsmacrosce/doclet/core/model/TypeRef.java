package com.jsmacrosce.doclet.core.model;

import com.jsmacrosce.doclet.DocletIgnore;

import java.util.List;

@DocletIgnore
public record TypeRef(
    TypeKind kind,
    String name,
    String qualifiedName,
    List<TypeRef> typeArgs,
    boolean array,
    boolean varArgs,
    TypeRef bounds,
    boolean nullable,
    WildcardBound wildcardBound
) {
    public enum WildcardBound {
        NONE,
        EXTENDS,
        SUPER
    }

    public TypeRef(
        TypeKind kind,
        String name,
        String qualifiedName,
        List<TypeRef> typeArgs,
        boolean array,
        boolean varArgs,
        TypeRef bounds,
        boolean nullable
    ) {
        this(
            kind,
            name,
            qualifiedName,
            typeArgs,
            array,
            varArgs,
            bounds,
            nullable,
            kind == TypeKind.WILDCARD && bounds != null ? WildcardBound.EXTENDS : WildcardBound.NONE
        );
    }

    public TypeRef withNullable(boolean nullable) {
        if (this.nullable == nullable) {
            return this;
        }
        return new TypeRef(kind, name, qualifiedName, typeArgs, array, varArgs, bounds, nullable, wildcardBound);
    }
}
