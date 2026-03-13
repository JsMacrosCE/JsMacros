package com.jsmacrosce.doclet.core.model;

import com.jsmacrosce.doclet.DocletIgnore;

import java.util.List;

@DocletIgnore
public record DocletModel(List<PackageDoc> packages, List<DeclaredTypeDoc> declaredTypes) {
}
