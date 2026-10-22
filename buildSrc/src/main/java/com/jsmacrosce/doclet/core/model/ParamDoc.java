package com.jsmacrosce.doclet.core.model;

import com.jsmacrosce.doclet.DocletIgnore;

@DocletIgnore
public record ParamDoc(String name, TypeRef type, boolean varArgs, String description) {
}
