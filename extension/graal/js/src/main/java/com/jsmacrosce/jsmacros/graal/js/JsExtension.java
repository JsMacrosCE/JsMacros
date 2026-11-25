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
 */
@DocletIgnore
public class JsExtension implements Extension {

    @Override
    public String getExtensionName() {
        return "graaljs";
    }

    @Override
    public void init(Core<?, ?> runner) {
        // The shared Graal extension warms up the runtime once all dependencies are available.
    }

}
