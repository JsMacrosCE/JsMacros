package com.jsmacrosce.jsmacros.graal.python;

import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.extensions.Extension;

/**
 * Marks that the GraalPython runtime is shipped alongside this build.
 *
 * <p>The language implementation itself lives in the shared {@code graal} extension; this class
 * exists so a build can include or omit the Python runtime independently of the Graal
 * infrastructure.
 */
@DocletIgnore
public class PythonExtension implements Extension {

    @Override
    public String getExtensionName() {
        return "graalpy";
    }

    @Override
    public void init(Core<?, ?> runner) {
        // The shared Graal extension warms up the runtime once all dependencies are available.
    }

}
