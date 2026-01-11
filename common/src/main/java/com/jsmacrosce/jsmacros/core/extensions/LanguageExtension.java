package com.jsmacrosce.jsmacros.core.extensions;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.language.BaseLanguage;
import com.jsmacrosce.jsmacros.core.language.BaseWrappedException;

import java.io.File;

public interface LanguageExtension extends Extension {

    int getPriority();

    ExtMatch extensionMatch(File file);

    String defaultFileExtension();

    /**
     * @return a single static instance of the language definition
     */
    BaseLanguage<?, ?> getLanguage(Core<?, ?> runner);

    BaseWrappedException<?> wrapException(Throwable t);

    boolean isGuestObject(Object o);

    enum ExtMatch {
        NOT_MATCH(false),
        MATCH(true),
        MATCH_WITH_NAME(true);

        boolean match;

        ExtMatch(boolean match) {
            this.match = match;
        }

        public boolean isMatch() {
            return match;
        }
    }
}
