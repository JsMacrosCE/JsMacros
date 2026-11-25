package com.jsmacrosce.jsmacros.graal.js;

import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.extensions.Extension;

/**
 * Marks that the JavaScript runtime is shipped alongside this build.
 *
 * <p>The language implementation itself lives in the shared {@code graal} extension; this class
 * exists so a build can include or omit the JavaScript runtime independently of the Graal
 * infrastructure.
 *
 * <p>It used to start a throwaway context during {@link #init} to warm up class loading. That
 * context created its own engine rather than the one scripts use, so it warmed only the classes
 * both engines share, and it cost a background thread plus a second engine. Script runtimes now
 * warm up on first use; the test harness performs that first use explicitly instead of sleeping.
 */
@DocletIgnore
public class JsExtension implements Extension {

    @Override
    public String getExtensionName() {
        return "graaljs";
    }

    @Override
    public void init(Core<?, ?> runner) {
        // Nothing to initialize: the language is provided by the graal extension.
    }

}
