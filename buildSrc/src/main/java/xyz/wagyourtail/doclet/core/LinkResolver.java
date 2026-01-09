package xyz.wagyourtail.doclet.core;

import xyz.wagyourtail.doclet.core.model.LinkRef;

import javax.lang.model.element.Element;

public interface LinkResolver {
    LinkRef resolve(Element element);
}
