package com.jsmacrosce.doclet.core.model;

import java.util.List;

public record PackageDoc(String name, List<ClassDoc> classes) {
}
