package com.jsmacrosce.jsmacros.test.extension;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.extensions.Extension;

/** An extension that is expected to be discovered by the loader. */
public class LoadedExtension implements Extension {

    /** Set by {@link #init(Core)} so the test can tell that the extension was initialized. */
    public static boolean initialized = false;

    @Override
    public String getExtensionName() {
        return "loaded";
    }

    @Override
    public void init(Core<?, ?> runner) {
        initialized = true;
    }

}
