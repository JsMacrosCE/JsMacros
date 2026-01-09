package xyz.wagyourtail.doclet.core.model;

import java.util.List;

public record DocletModel(List<PackageDoc> packages, List<DeclaredTypeDoc> declaredTypes) {
}
