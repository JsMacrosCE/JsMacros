package com.jsmacrosce.doclet.core.model;

public record ParamDoc(String name, TypeRef type, boolean varArgs, String description) {
}
