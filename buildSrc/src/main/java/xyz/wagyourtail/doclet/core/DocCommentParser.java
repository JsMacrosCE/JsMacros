package xyz.wagyourtail.doclet.core;

import xyz.wagyourtail.doclet.core.model.DocComment;

import javax.lang.model.element.Element;

public interface DocCommentParser {
    DocComment parse(Element element);
}
