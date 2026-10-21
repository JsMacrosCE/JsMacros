package com.jsmacrosce.jsmacros.core.extensions;

import com.jsmacrosce.jsmacros.core.library.BaseLibrary;

import java.util.Set;

/**
 * An extension provider that contributes library classes for registration. The loader calls
 * {@link Extension#init(com.jsmacrosce.jsmacros.core.Core)} before registering these classes; each
 * class must have a {@link com.jsmacrosce.jsmacros.core.library.Library} annotation.
 * <pre>
 * const extensions = context.getCtx().runner.extensions.getAllLibraryExtensions().iterator();
 * while (extensions.hasNext()) {
 *   const extension = extensions.next();
 *   Chat.log(extension.getExtensionName() + ": " + extension.getLibraries().size());
 * }
 * </pre>
 */
public interface LibraryExtension extends Extension {

    /**
     * Returns this extension's library classes for registration; an empty set means it contributes
     * no libraries. Return a non-null set containing non-null classes with {@code @Library}
     * annotations for loader use.
     *
     * @return the library classes for registration, or an empty set if this extension contributes none
     */
    Set<Class<? extends BaseLibrary>> getLibraries();

}
