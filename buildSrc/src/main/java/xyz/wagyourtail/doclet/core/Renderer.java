package xyz.wagyourtail.doclet.core;

import xyz.wagyourtail.doclet.core.model.DocletModel;

public interface Renderer {
    String render(DocletModel model);
}
