package com.jsmacrosce.jsmacros.core.extensions;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Loads extension classes and resources from jar files on a URL search path.
 *
 * <p>Every archive this loader reads exists as a file first. The extension loader copies a nested
 * archive, such as a dependency inside an extension jar, out of its container before adding it
 * here, because a {@link URLClassLoader} cannot open an archive that is itself an entry of another
 * archive.
 *
 * <p>The parent is the class loader that defined {@code ExtensionClassLoader}; class loading
 * otherwise follows the inherited parent-first delegation model, so an extension always uses the
 * core's own classes.
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
     * Appends a jar file to this loader's search path, for example an extension dependency that
     * has already been extracted from its parent archive.
     * <p>
     * A {@code null} URL and a URL that is already on the path are ignored.
     *
     * @param url URL to append; {@code null} is ignored
     */
    @Override
    public synchronized void addURL(URL url) {
        if (url == null) {
            return;
        }
        for (URL existing : getURLs()) {
            if (existing.toExternalForm().equals(url.toExternalForm())) {
                return;
            }
        }
        super.addURL(url);
    }

}
