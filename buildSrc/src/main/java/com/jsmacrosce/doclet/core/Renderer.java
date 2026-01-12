package com.jsmacrosce.doclet.core;

import com.jsmacrosce.doclet.core.model.DocletModel;

public interface Renderer {
    String render(DocletModel model);
}
