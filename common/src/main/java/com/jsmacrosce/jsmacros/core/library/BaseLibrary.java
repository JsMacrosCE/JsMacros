package com.jsmacrosce.jsmacros.core.library;

import com.jsmacrosce.jsmacros.core.Core;

public abstract class BaseLibrary {
    public Core<?, ?> runner;

    public BaseLibrary(Core<?, ?> runner) {
        this.runner = runner;
    }

}
