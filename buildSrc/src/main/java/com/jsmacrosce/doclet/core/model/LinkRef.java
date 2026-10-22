package com.jsmacrosce.doclet.core.model;

import com.jsmacrosce.doclet.DocletIgnore;

@DocletIgnore
public record LinkRef(String url, boolean external) {
}
