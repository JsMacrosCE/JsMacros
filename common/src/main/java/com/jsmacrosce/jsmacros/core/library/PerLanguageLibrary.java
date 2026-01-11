package com.jsmacrosce.jsmacros.core.library;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.language.BaseLanguage;

public abstract class PerLanguageLibrary extends BaseLibrary {
    protected Class<? extends BaseLanguage<?, ?>> language;

    public PerLanguageLibrary(Core<?, ?> runner, Class<? extends BaseLanguage<?, ?>> language) {
        super(runner);
        this.language = language;
    }

}
