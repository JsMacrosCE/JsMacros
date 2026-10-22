package com.jsmacrosce.doclet.core.model;

import com.jsmacrosce.doclet.DocletIgnore;

import java.util.List;

@DocletIgnore
public record DocletModel(
    List<PackageDoc> packages,
    List<DeclaredTypeDoc> declaredTypes,
    /**
     * Types the documented API refers to that are not documented themselves, such as the
     * library and platform types a signature mentions. Only the TypeScript definitions use
     * them: a reference to such a type has to resolve to something.
     */
    List<ClassDoc> referencedTypes,
    /**
     * The types the API refers to that are not declared, because they are not part of the
     * documented API. A reference to one of them has to say so rather than point at nothing.
     */
    java.util.Set<String> undeclaredTypes
) {
    public DocletModel(List<PackageDoc> packages, List<DeclaredTypeDoc> declaredTypes) {
        this(packages, declaredTypes, List.of(), java.util.Set.of());
    }

    public DocletModel(
        List<PackageDoc> packages,
        List<DeclaredTypeDoc> declaredTypes,
        List<ClassDoc> referencedTypes
    ) {
        this(packages, declaredTypes, referencedTypes, java.util.Set.of());
    }
}
