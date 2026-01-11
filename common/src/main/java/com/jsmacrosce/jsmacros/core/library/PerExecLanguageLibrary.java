package com.jsmacrosce.jsmacros.core.library;

import com.jsmacrosce.jsmacros.core.language.BaseLanguage;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;

public class PerExecLanguageLibrary<U, T extends BaseScriptContext<U>> extends BaseLibrary {
    protected final T ctx;
    protected final Class<? extends BaseLanguage<U, T>> language;

    public PerExecLanguageLibrary(T context, Class<? extends BaseLanguage<U, T>> language) {
        super(context.runner);
        this.language = language;
        this.ctx = context;
    }

}
