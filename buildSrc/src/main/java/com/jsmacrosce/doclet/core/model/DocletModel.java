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
    List<ClassDoc> referencedTypes
) {
    public DocletModel(List<PackageDoc> packages, List<DeclaredTypeDoc> declaredTypes) {
        this(packages, declaredTypes, List.of());
    }
}
