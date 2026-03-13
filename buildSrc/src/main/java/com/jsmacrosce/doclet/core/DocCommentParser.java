package com.jsmacrosce.doclet.core;

import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.doclet.core.model.DocComment;

import javax.lang.model.element.Element;

@DocletIgnore
public interface DocCommentParser {
    DocComment parse(Element element);
}
