package com.jsmacrosce.jsmacros.core.library;

import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;

public abstract class PerExecLibrary extends BaseLibrary {
    protected BaseScriptContext<?> ctx;

    public PerExecLibrary(BaseScriptContext<?> context) {
        super(context.runner);
        this.ctx = context;
    }

}
