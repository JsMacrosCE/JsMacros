package com.jsmacrosce.jsmacros.core.extensions;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Loads classes from an initial URL search path and allows URLs to be appended.
 * Its parent is the class loader that defined {@code ExtensionClassLoader};
 * class loading otherwise follows the inherited parent-first delegation model.
 * <pre>
 * const Loader = Java.type("com.jsmacrosce.jsmacros.core.extensions.ExtensionClassLoader");
 * const urls = Java.to([], "java.net.URL[]");
 * const loader = new Loader(urls);
 * try {
 *   loader.loadClass("java.lang.String");
 * } finally {
 *   loader.close();
 * }
 * </pre>
 */
public class ExtensionClassLoader extends URLClassLoader {

    /**
     * Sets the initial URL search path and uses the loader that defined this class as parent.
     * @param urls initial URLs; the array must not be {@code null}, but may be empty
     */
    public ExtensionClassLoader(URL[] urls) {
        super(urls, ExtensionClassLoader.class.getClassLoader());
    }

    /**
     * Appends a URL to this loader's search path. A {@code null} URL, a duplicate URL,
     * or an addition after the loader is closed is ignored.
     * @param url URL to append; {@code null} is ignored
     */
    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

}
