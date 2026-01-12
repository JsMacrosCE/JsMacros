package com.jsmacrosce.doclet.core;

import com.jsmacrosce.doclet.core.model.LinkRef;

import javax.lang.model.element.Element;

public interface LinkResolver {
    LinkRef resolve(Element element);
}
