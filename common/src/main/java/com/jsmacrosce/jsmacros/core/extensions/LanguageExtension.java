package com.jsmacrosce.jsmacros.core.extensions;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.language.BaseLanguage;
import com.jsmacrosce.jsmacros.core.language.BaseWrappedException;

import java.io.File;

/**
 * Provides a language implementation and file, exception, and guest-object handling to the extension system.
 * <pre>
 * const languages = context.getCtx().runner.extensions.getAllLanguageExtensions().iterator();
 * while (languages.hasNext()) {
 *   const language = languages.next();
 *   Chat.log(language.getExtensionName() + ": " + language.defaultFileExtension());
 * }
 * </pre>
 */
public interface LanguageExtension extends Extension {

    /**
     * Returns the priority used to select a language extension.
     * Higher values are normally preferred; file-match sorting negates this value, so {@code Integer.MIN_VALUE}
     * overflows and cannot serve as the lowest file-matching priority. Equal priorities have no guaranteed order.
     * @return priority used for selection
     */
    int getPriority();

    /**
     * Classifies whether this extension supports a file; the result describes this extension, not the loader's final selection.
     * @param file candidate file
     * @return {@link ExtMatch#NOT_MATCH} if unsupported, {@link ExtMatch#MATCH} for a match, or
     *         {@link ExtMatch#MATCH_WITH_NAME} for a language-name match preferred over ordinary matches
     */
    ExtMatch extensionMatch(File file);

    /**
     * Returns the default suffix used when creating a new file, without a leading period (for example, {@code "js"}).
     * @return default suffix without a leading period
     */
    String defaultFileExtension();

    /**
     * Returns a language definition for the supplied core.
     * @param runner the core for which to obtain a language definition
     * @return the language definition associated with {@code runner}
     */
    BaseLanguage<?, ?> getLanguage(Core<?, ?> runner);

    /**
     * Returns a language-specific diagnostic wrapper when this extension recognizes the throwable.
     * @param t throwable to inspect
     * @return language-specific wrapper, or {@code null} if unrecognized
     */
    BaseWrappedException<?> wrapException(Throwable t);

    /**
     * Tests whether this extension recognizes an object as a language-specific guest representation.
     * @param o value to inspect
     * @return {@code true} if recognized as a guest object; {@code false} otherwise
     */
    boolean isGuestObject(Object o);

    /**
     * Categorizes an extension's match for a file. A name match takes precedence over ordinary matches when the loader
     * chooses among matching providers.
     * <pre>
     * const ExtMatch = Java.type("com.jsmacrosce.jsmacros.core.extensions.LanguageExtension$ExtMatch");
     * Chat.log(ExtMatch.MATCH_WITH_NAME.isMatch());
     * </pre>
     */
    enum ExtMatch {
        /** The extension does not match the file. */
        NOT_MATCH(false),
        /** The extension matches the file. */
        MATCH(true),
        /** The extension matches the file by language name. */
        MATCH_WITH_NAME(true);

        boolean match;

        ExtMatch(boolean match) {
            this.match = match;
        }

        /**
         * Returns whether this category represents a match.
         * @return {@code false} for {@link #NOT_MATCH}; {@code true} for either match category
         */
        public boolean isMatch() {
            return match;
        }
    }
}
