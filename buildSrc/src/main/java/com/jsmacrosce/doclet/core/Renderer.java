package com.jsmacrosce.doclet.core;

import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.doclet.core.model.DocletModel;

@DocletIgnore
public interface Renderer {
    String render(DocletModel model);
}
