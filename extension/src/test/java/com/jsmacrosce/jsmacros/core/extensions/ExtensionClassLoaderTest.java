package com.jsmacrosce.jsmacros.core.extensions;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtNewMethod;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises {@link ExtensionClassLoader} with ordinary jar files, which is the only kind of
 * archive it reads. The extension loader extracts a nested archive before handing it to this
 * class loader; that extraction is covered by {@link ExtensionLoaderIntegrationTest}.
 *
 * <p>Jars are written under {@code build/} rather than in a temp directory: the class loader keeps
 * them open, and on Windows that would make deleting the directory fail.
 */
class ExtensionClassLoaderTest {

    /** Service interface used to check that service files inside a jar are found. */
    public interface Greeter {
        String greet();
    }

    private final Path workDir = Path.of("build", "test-extension-classloader", UUID.randomUUID().toString());

    private static byte[] classBytes(String className, String body) throws Exception {
        ClassPool pool = new ClassPool(true);
        CtClass ct = pool.makeClass(className);
        ct.addMethod(CtNewMethod.make(body, ct));
        return ct.toBytecode();
    }

    private static byte[] serviceClassBytes(String className, String body) throws Exception {
        ClassPool pool = new ClassPool(true);
        CtClass ct = pool.makeClass(className);
        ct.addInterface(pool.get(Greeter.class.getName()));
        ct.addMethod(CtNewMethod.make(body, ct));
        return ct.toBytecode();
    }

    private record Entry(String name, byte[] content) {
    }

    private static Entry entry(String name, byte[] content) {
        return new Entry(name, content);
    }

    private Path writeJar(String fileName, Entry... entries) throws IOException {
        Files.createDirectories(workDir);
        Path jar = workDir.resolve(fileName);
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            for (Entry entry : entries) {
                out.putNextEntry(new JarEntry(entry.name()));
                out.write(entry.content());
                out.closeEntry();
            }
        }
        return jar;
    }

    @Test
    void loadsAClassAndResourceFromAJarOnTheSearchPath() throws Exception {
        byte[] bytes = classBytes("com.example.jar.Widget",
                "public String describe() { return \"a widget\"; }");
        Path jar = writeJar("widget.jar",
                entry("com/example/jar/Widget.class", bytes),
                entry("data/hello.txt", "hello from a jar".getBytes(StandardCharsets.UTF_8)));

        try (ExtensionClassLoader loader = new ExtensionClassLoader(new URL[]{jar.toUri().toURL()})) {
            Class<?> loaded = loader.loadClass("com.example.jar.Widget");
            assertEquals("com.example.jar.Widget", loaded.getName());
            assertSame(loader, loaded.getClassLoader());
            assertEquals("a widget", loaded.getMethod("describe").invoke(loaded.getDeclaredConstructor().newInstance()));

            URL resource = loader.getResource("data/hello.txt");
            assertNotNull(resource);
            try (InputStream in = resource.openStream()) {
                assertEquals("hello from a jar", new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
    }

    @Test
    void findsServiceFilesInsideAJarOnTheSearchPath() throws Exception {
        byte[] bytes = serviceClassBytes("com.example.jar.GreeterImpl",
                "public String greet() { return \"hello\"; }");
        Path jar = writeJar("services.jar",
                entry("com/example/jar/GreeterImpl.class", bytes),
                entry("META-INF/services/" + Greeter.class.getName(),
                        "com.example.jar.GreeterImpl\n".getBytes(StandardCharsets.UTF_8)));

        try (ExtensionClassLoader loader = new ExtensionClassLoader(new URL[]{jar.toUri().toURL()})) {
            boolean found = false;
            for (Greeter greeter : ServiceLoader.load(Greeter.class, loader)) {
                assertEquals("hello", greeter.greet());
                found = true;
            }
            assertTrue(found, "the service provider should have been discovered");
        }
    }

    @Test
    void appendsAJarWithAddUrl() throws Exception {
        Path first = writeJar("first.jar",
                entry("data/first.txt", "first".getBytes(StandardCharsets.UTF_8)));
        byte[] bytes = classBytes("com.example.jar.Added",
                "public static String value() { return \"added\"; }");
        Path second = writeJar("second.jar",
                entry("com/example/jar/Added.class", bytes),
                entry("data/second.txt", "second".getBytes(StandardCharsets.UTF_8)));

        try (ExtensionClassLoader loader = new ExtensionClassLoader(new URL[]{first.toUri().toURL()})) {
            assertNull(loader.getResource("data/second.txt"),
                    "the second jar is not on the search path yet");

            loader.addURL(second.toUri().toURL());

            URL resource = loader.getResource("data/second.txt");
            assertNotNull(resource);
            try (InputStream in = resource.openStream()) {
                assertEquals("second", new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
            Class<?> loaded = loader.loadClass("com.example.jar.Added");
            assertEquals("added", loaded.getMethod("value").invoke(null));
        }
    }

    @Test
    void ignoresNullAndDuplicateUrls() throws Exception {
        Path first = writeJar("first.jar",
                entry("data/first.txt", "first".getBytes(StandardCharsets.UTF_8)));
        Path second = writeJar("second.jar",
                entry("data/second.txt", "second".getBytes(StandardCharsets.UTF_8)));

        try (ExtensionClassLoader loader = new ExtensionClassLoader(new URL[]{first.toUri().toURL()})) {
            assertEquals(1, loader.getURLs().length);
            loader.addURL(second.toUri().toURL());
            assertEquals(2, loader.getURLs().length);

            loader.addURL(null);
            loader.addURL(second.toUri().toURL());
            loader.addURL(first.toUri().toURL());

            assertEquals(2, loader.getURLs().length, "null and duplicate URLs should be ignored");
        }
    }

    @Test
    void keepsUsingTheParentLoaderForClassesItDoesNotHold() throws Exception {
        Path jar = writeJar("empty.jar");
        try (ExtensionClassLoader loader = new ExtensionClassLoader(new URL[]{jar.toUri().toURL()})) {
            assertEquals(String.class, loader.loadClass("java.lang.String"));
            assertNull(loader.getResource("data/does-not-exist.txt"));
        }
    }

}
