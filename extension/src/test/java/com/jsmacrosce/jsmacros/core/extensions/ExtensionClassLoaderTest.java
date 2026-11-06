package com.jsmacrosce.jsmacros.core.extensions;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtNewMethod;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ServiceLoader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtensionClassLoaderTest {

    /** Service interface used to check that service files inside a memory archive are found. */
    public interface Greeter {
        String greet();
    }

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

    private static byte[] archive(Entry... entries) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Entry entry : entries) {
                zip.putNextEntry(new ZipEntry(entry.name()));
                zip.write(entry.content());
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private static Entry entry(String name, byte[] content) {
        return new Entry(name, content);
    }

    @Test
    void loadsAClassFromAnArchiveHeldInMemory() throws Exception {
        byte[] bytes = classBytes("com.example.memory.Widget",
                "public String describe() { return \"a widget\"; }");
        ExtensionClassLoader loader = new ExtensionClassLoader(new URL[0]);
        loader.addMemoryJar("widget.jar", archive(entry("com/example/memory/Widget.class", bytes)));

        Class<?> loaded = loader.loadClass("com.example.memory.Widget");
        assertEquals("com.example.memory.Widget", loaded.getName());
        assertSame(loader, loaded.getClassLoader());
        assertEquals("a widget", loaded.getMethod("describe").invoke(loaded.getDeclaredConstructor().newInstance()));
    }

    @Test
    void readsAResourceFromAnArchiveHeldInMemory() throws Exception {
        ExtensionClassLoader loader = new ExtensionClassLoader(new URL[0]);
        loader.addMemoryJar("data.jar", archive(entry("data/hello.txt", "hello from memory".getBytes())));

        URL resource = loader.getResource("data/hello.txt");
        assertNotNull(resource);
        try (InputStream in = resource.openStream()) {
            assertEquals("hello from memory", new String(in.readAllBytes()));
        }
    }

    @Test
    void findsServiceFilesInsideAnArchiveHeldInMemory() throws Exception {
        byte[] bytes = serviceClassBytes("com.example.memory.GreeterImpl",
                "public String greet() { return \"hello\"; }");
        ExtensionClassLoader loader = new ExtensionClassLoader(new URL[0]);
        loader.addMemoryJar("services.jar", archive(
                entry("com/example/memory/GreeterImpl.class", bytes),
                entry("META-INF/services/" + Greeter.class.getName(),
                        "com.example.memory.GreeterImpl\n".getBytes())));

        boolean found = false;
        for (Greeter greeter : ServiceLoader.load(Greeter.class, loader)) {
            assertEquals("hello", greeter.greet());
            found = true;
        }
        assertTrue(found, "the service provider should have been discovered");
    }

    @Test
    void servesAnArchiveNestedInsideAnotherArchive() throws Exception {
        byte[] innerBytes = classBytes("com.example.memory.Nested",
                "public String describe() { return \"nested\"; }");
        byte[] inner = archive(entry("com/example/memory/Nested.class", innerBytes));
        byte[] outer = archive(entry("META-INF/jars/inner.jar", inner));

        ExtensionClassLoader loader = new ExtensionClassLoader(new URL[0]);
        loader.addMemoryJar("outer.jar", outer);

        // The nested archive is reached through the outer one, exactly as the loader does it.
        InMemoryJar outerJar = new InMemoryJar(outer, "outer.jar");
        byte[] nested = outerJar.read("META-INF/jars/inner.jar");
        assertNotNull(nested);

        ExtensionClassLoader nestedLoader = new ExtensionClassLoader(new URL[0]);
        nestedLoader.addMemoryJar("inner.jar", nested);
        Class<?> loaded = nestedLoader.loadClass("com.example.memory.Nested");
        assertEquals("nested", loaded.getMethod("describe").invoke(loaded.getDeclaredConstructor().newInstance()));
    }

    /**
     * Mirrors the layout a shipped extension uses: the mod jar holds the extension archive, and
     * the extension archive holds the extension class, its service file, its dependency
     * descriptor, and the archive of the extension's own dependency.
     */
    @Test
    void servesTheWholeChainOfNestedExtensionArchives() throws Exception {
        byte[] dependencyBytes = classBytes("com.example.memory.Dependency",
                "public static String value() { return \"dependency\"; }");
        byte[] dependencyArchive = archive(entry("com/example/memory/Dependency.class", dependencyBytes));

        byte[] extensionBytes = serviceClassBytes("com.example.memory.GreeterImpl",
                "public String greet() { return \"hello\"; }");
        byte[] extensionArchive = archive(
                entry("com/example/memory/GreeterImpl.class", extensionBytes),
                entry("META-INF/services/" + Greeter.class.getName(), "com.example.memory.GreeterImpl\n".getBytes()),
                entry("jsmacrosce.ext.testarchive.json",
                        "{\"dependencies\": [\"META-INF/jsmacroscedeps/dependency.jar\"]}".getBytes()),
                entry("META-INF/jsmacroscedeps/dependency.jar", dependencyArchive));

        byte[] modArchive = archive(entry("META-INF/jsmacroscedeps/testext.jar", extensionArchive));

        // The mod jar's manifest names the extension archive.
        ExtensionClassLoader loader = new ExtensionClassLoader(new URL[0]);
        InMemoryJar modJar = new InMemoryJar(modArchive, "mod.jar");
        byte[] extension = modJar.read("META-INF/jsmacroscedeps/testext.jar");
        assertNotNull(extension);
        loader.addMemoryJar("testext.jar", extension);

        // The extension is discovered from the service file inside the extension archive.
        boolean found = false;
        for (Greeter greeter : ServiceLoader.load(Greeter.class, loader)) {
            assertEquals("hello", greeter.greet());
            found = true;
        }
        assertTrue(found, "the service provider should have been discovered in the nested archive");

        // Its dependency descriptor is readable from the same archive.
        URL descriptor = loader.getResource("jsmacrosce.ext.testarchive.json");
        assertNotNull(descriptor);

        // The dependency archive it names is readable from inside the extension archive.
        URL dependency = loader.getResource("META-INF/jsmacroscedeps/dependency.jar");
        assertNotNull(dependency);
        byte[] dependencyJar;
        try (InputStream in = dependency.openStream()) {
            dependencyJar = in.readAllBytes();
        }

        ExtensionClassLoader withDependency = new ExtensionClassLoader(new URL[0]);
        withDependency.addMemoryJar("testext.jar", extension);
        withDependency.addMemoryJar("dependency.jar", dependencyJar);
        Class<?> loaded = withDependency.loadClass("com.example.memory.Dependency");
        assertEquals("dependency", loaded.getMethod("value").invoke(null));
    }

    @Test
    void keepsUsingTheParentLoaderForClassesItDoesNotHold() throws Exception {
        ExtensionClassLoader loader = new ExtensionClassLoader(new URL[0]);
        loader.addMemoryJar("empty.jar", archive());

        assertEquals(String.class, loader.loadClass("java.lang.String"));
        assertNull(loader.getResource("data/does-not-exist.txt"));
    }

}
