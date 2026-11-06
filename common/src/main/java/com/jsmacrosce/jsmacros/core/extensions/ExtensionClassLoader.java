package com.jsmacrosce.jsmacros.core.extensions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Loads extension classes and resources.
 *
 * <p>Two kinds of archive are served. Archives that exist as files, such as the jars a user
 * drops into the {@code Extensions} directory, are read through the inherited URL search path.
 * Archives that are nested inside another archive, such as the extension jars shipped inside the
 * mod jar, are read from memory because the JDK cannot open a jar that sits inside a jar.
 *
 * <p>Its parent is the class loader that defined {@code ExtensionClassLoader}; class loading
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

    private final List<InMemoryJar> memoryJars = new ArrayList<>();

    /**
     * Sets the initial URL search path and uses the loader that defined this class as parent.
     * @param urls initial URLs; the array must not be {@code null}, but may be empty
     */
    public ExtensionClassLoader(URL[] urls) {
        super(urls, ExtensionClassLoader.class.getClassLoader());
    }

    /**
     * Adds an archive that is held in memory to this loader's search path. In-memory archives are
     * consulted before the file search path when a class or resource is requested.
     *
     * @param name  archive name, used in resource URLs and diagnostics
     * @param bytes the complete archive bytes
     * @throws IOException if the bytes are not a readable archive
     */
    public void addMemoryJar(String name, byte[] bytes) throws IOException {
        memoryJars.add(new InMemoryJar(bytes, name));
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> found = findInMemoryClass(name);
        return found != null ? found : super.findClass(name);
    }

    @Override
    public URL findResource(String name) {
        URL found = findInMemoryResource(name);
        return found != null ? found : super.findResource(name);
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        List<URL> found = new ArrayList<>();
        for (InMemoryJar jar : memoryJars) {
            if (!jar.contains(name)) {
                continue;
            }
            byte[] bytes = jar.read(name);
            if (bytes != null) {
                URL url = memoryUrl(jar, name, bytes);
                if (url != null) {
                    found.add(url);
                }
            }
        }
        Enumeration<URL> fromFiles = super.findResources(name);
        if (found.isEmpty()) {
            return fromFiles;
        }
        found.addAll(Collections.list(fromFiles));
        return Collections.enumeration(found);
    }

    private Class<?> findInMemoryClass(String name) {
        String entry = name.replace('.', '/') + ".class";
        for (InMemoryJar jar : memoryJars) {
            byte[] bytes;
            try {
                bytes = jar.read(entry);
            } catch (IOException e) {
                continue;
            }
            if (bytes == null) {
                continue;
            }
            definePackageIfAbsent(name);
            return defineClass(name, bytes, 0, bytes.length);
        }
        return null;
    }

    private URL findInMemoryResource(String name) {
        for (InMemoryJar jar : memoryJars) {
            if (!jar.contains(name)) {
                continue;
            }
            try {
                byte[] bytes = jar.read(name);
                if (bytes != null) {
                    URL url = memoryUrl(jar, name, bytes);
                    if (url != null) {
                        return url;
                    }
                }
            } catch (IOException e) {
                // A damaged archive entry is treated as a missing resource.
            }
        }
        return null;
    }

    private void definePackageIfAbsent(String className) {
        int lastDot = className.lastIndexOf('.');
        if (lastDot < 0) {
            return;
        }
        String packageName = className.substring(0, lastDot);
        if (getDefinedPackage(packageName) == null) {
            try {
                definePackage(packageName, null, null, null, null, null, null, null);
            } catch (IllegalArgumentException e) {
                // Defined concurrently; nothing to do.
            }
        }
    }

    private static URL memoryUrl(InMemoryJar jar, String entry, byte[] bytes) {
        try {
            return new URL(null, "jsmacros-extension:" + jar.name() + "!/" + entry, new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL url) {
                    return new URLConnection(url) {
                        @Override
                        public void connect() {
                        }

                        @Override
                        public InputStream getInputStream() {
                            return new ByteArrayInputStream(bytes);
                        }
                    };
                }
            });
        } catch (MalformedURLException e) {
            return null;
        }
    }

}
