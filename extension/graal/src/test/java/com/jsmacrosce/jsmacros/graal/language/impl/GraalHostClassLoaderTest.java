package com.jsmacrosce.jsmacros.graal.language.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraalHostClassLoaderTest {
    @TempDir
    Path tempDir;

    @Test
    void discoversServicesRegisteredOnlyInTheExtension() throws Exception {
        String descriptor = "META-INF/services/" + Greeting.class.getName();
        Path serviceFile = tempDir.resolve(descriptor);
        Files.createDirectories(serviceFile.getParent());
        Files.writeString(serviceFile, ExtensionGreeting.class.getName() + "\n");

        ClassLoader reflectionLoader = getClass().getClassLoader();
        try (URLClassLoader extensionLoader = new URLClassLoader(new URL[]{tempDir.toUri().toURL()}, reflectionLoader)) {
            GraalHostClassLoader hostLoader = new GraalHostClassLoader(reflectionLoader, extensionLoader, UnaryOperator.identity());

            assertNull(reflectionLoader.getResource(descriptor));
            assertEquals(extensionLoader.getResource(descriptor), hostLoader.getResource(descriptor));
            assertEquals("hello", ServiceLoader.load(Greeting.class, hostLoader).findFirst().orElseThrow().greet());
        }
    }

    @Test
    void searchesReflectionResourcesFirstWithoutRepeatingSharedResources() throws Exception {
        String name = "graal-host-loader-test.txt";
        Path reflectionDir = Files.createDirectory(tempDir.resolve("reflection"));
        Path extensionDir = Files.createDirectory(tempDir.resolve("extension"));
        URL reflectionResource = Files.writeString(reflectionDir.resolve(name), "reflection").toUri().toURL();
        URL extensionResource = Files.writeString(extensionDir.resolve(name), "extension").toUri().toURL();

        try (URLClassLoader reflectionLoader = new URLClassLoader(new URL[]{reflectionDir.toUri().toURL()});
             URLClassLoader extensionLoader = new URLClassLoader(new URL[]{extensionDir.toUri().toURL()}, reflectionLoader)) {
            GraalHostClassLoader hostLoader = new GraalHostClassLoader(reflectionLoader, extensionLoader, UnaryOperator.identity());

            assertEquals(reflectionResource, hostLoader.getResource(name));
            assertEquals(List.of(reflectionResource, extensionResource), Collections.list(hostLoader.getResources(name)));
            assertTrue(Collections.list(hostLoader.getResources("missing.txt")).isEmpty());
        }
    }

    public interface Greeting {
        String greet();
    }

    public static class ExtensionGreeting implements Greeting {
        @Override
        public String greet() {
            return "hello";
        }
    }
}
