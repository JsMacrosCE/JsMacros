package com.jsmacrosce.jsmacros.core.library;

import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;

@DocletIgnore
public abstract class PerExecLibrary extends BaseLibrary {
    protected BaseScriptContext<?> ctx;

    public PerExecLibrary(BaseScriptContext<?> context) {
        super(context.runner);
        this.ctx = context;
    }

}
