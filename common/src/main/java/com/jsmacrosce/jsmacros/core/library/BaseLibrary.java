package com.jsmacrosce.jsmacros.core.library;

import com.jsmacrosce.jsmacros.core.Core;

/**
 * Base class for registered script libraries. The registry constructs instances per profile by
 * default; subclasses can instead use per-language or per-execution lifetimes.
 */
public abstract class BaseLibrary {
    /**
     * Core instance used to construct this library. It is exposed through the public field, but is
     * internal infrastructure rather than a supported script API.
     */
    public Core<?, ?> runner;

    /**
     * Initializes this library with its associated core.
     * @param runner core associated with this library
     */
    public BaseLibrary(Core<?, ?> runner) {
        this.runner = runner;
    }

}
