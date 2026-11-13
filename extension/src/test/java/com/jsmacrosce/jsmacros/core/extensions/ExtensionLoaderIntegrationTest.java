package com.jsmacrosce.jsmacros.core.extensions;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.test.stubs.CoreInstanceCreator;
import com.jsmacrosce.jsmacros.test.stubs.EventRegistryStub;
import com.jsmacrosce.jsmacros.test.stubs.ProfileStub;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtNewMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Exercises the loader the way the game does: extension jars on disk, one of them carrying its
 * own dependency archive inside itself.
 *
 * <p>The other loader tests run providers from the test classpath, which the parent class loader
 * would find anyway; they never reach the archive handling. Here every provider comes from a jar
 * written by the test, so discovery, dependency loading from a nested archive, first-archive-wins
 * duplicate handling, and per-extension failure isolation are all covered end to end.
 *
 * <p>Working directories are created under {@code build/} rather than in a temp directory: the
 * loader keeps its jar files open, and on Windows that would make deleting the directory fail.
 */
class ExtensionLoaderIntegrationTest {

    private static final String SERVICE_FILE = "META-INF/services/" + Extension.class.getName();

    private final ClassPool pool = new ClassPool(true);
    private final Path workDir = Path.of("build", "test-extension-loader", UUID.randomUUID().toString());

    @BeforeEach
    void clearProbe() {
        IntegrationProbe.reset();
    }

    @Test
    void loadsAnExtensionAndTheDependencyArchiveInsideItsJar() throws Exception {
        writeExtensionJar("fixture.jar", fixtureJar());

        Core<ProfileStub, EventRegistryStub> core = core();

        assertNotNull(core.extensions.getExtensionForName("fixture"));
        assertEquals("ok", IntegrationProbe.value("fixture"),
                "the extension's init should be able to load a class from its nested dependency archive");
        assertEquals(1, IntegrationProbe.count("fixture"), "an extension should be initialized once");
    }

    @Test
    void keepsTheFirstArchiveWhenTwoDeclareTheSameExtensionName() throws Exception {
        writeExtensionJar("a-first.jar", dupeJar("first"));
        writeExtensionJar("b-second.jar", dupeJar("second"));

        Core<ProfileStub, EventRegistryStub> core = core();

        assertEquals(1, core.extensions.getAllExtensions().stream()
                .filter(e -> e.getExtensionName().equals("dupe")).count());
        assertEquals("first", IntegrationProbe.value("dupe"),
                "the archive whose file name sorts first should be the one that is initialized");
        assertEquals(1, IntegrationProbe.count("dupe"), "the ignored duplicate should not be initialized");
    }

    @Test
    void skipsAProviderThatCannotBeFoundAndKeepsLoadingTheRest() throws Exception {
        writeExtensionJar("fixture.jar", fixtureJar());
        writeExtensionJar("broken.jar", archive(entry(SERVICE_FILE,
                "com.example.fixture.DoesNotExist\n".getBytes(StandardCharsets.UTF_8))));

        Core<ProfileStub, EventRegistryStub> core = core();

        assertNotNull(core.extensions.getExtensionForName("fixture"));
        assertNull(core.extensions.getExtensionForName("broken"));
    }

    @Test
    void skipsAnExtensionWhoseInitThrowsAndKeepsLoadingTheRest() throws Exception {
        writeExtensionJar("fixture.jar", fixtureJar());
        writeExtensionJar("failing.jar", failingJar());

        Core<ProfileStub, EventRegistryStub> core = core();

        assertNotNull(core.extensions.getExtensionForName("fixture"),
                "a failing extension must not stop the ones after it");
        assertNull(core.extensions.getExtensionForName("failing"));
    }

    @Test
    void letsAnExtensionQueryTheLoaderDuringInit() throws Exception {
        writeExtensionJar("reentrant.jar", reentrantJar());

        Core<ProfileStub, EventRegistryStub> core = core();

        assertNotNull(core.extensions.getExtensionForName("reentrant"));
        assertEquals("ok", IntegrationProbe.value("reentrant"),
                "a query during init must return instead of starting another load");
        assertEquals(1, IntegrationProbe.count("reentrant"));
    }

    @Test
    void skipsAProviderWhoseNameCannotBeRead() throws Exception {
        writeExtensionJar("fixture.jar", fixtureJar());
        writeExtensionJar("unnamed.jar", throwingNameJar());

        Core<ProfileStub, EventRegistryStub> core = core();

        assertNotNull(core.extensions.getExtensionForName("fixture"),
                "an unreadable name must not abort the rest of the load");
        assertEquals("ok", IntegrationProbe.value("fixture"));
    }

    @Test
    void doesNotPublishDependenciesWhenALaterDependencyIsInvalid() throws Exception {
        writeExtensionJar("a-batch.jar", batchJar());
        writeExtensionJar("b-observer.jar", observerJar());

        Core<ProfileStub, EventRegistryStub> core = core();
        core.extensions.loadExtensions();

        assertNull(core.extensions.getExtensionForName("batch"));
        assertEquals("missing", IntegrationProbe.value("observer"),
                "a dependency from the failed batch must not remain visible");
    }

    @Test
    void loadsExtensionsOncePerLoader() throws Exception {
        writeExtensionJar("fixture.jar", fixtureJar());
        Core<ProfileStub, EventRegistryStub> core = core();

        core.extensions.loadExtensions();
        int size = core.extensions.getAllExtensions().size();
        core.extensions.loadExtensions();

        assertEquals(size, core.extensions.getAllExtensions().size(), "a second load should add nothing");
        assertEquals(1, IntegrationProbe.count("fixture"), "a second load should not re-initialize an extension");
    }

    private Core<ProfileStub, EventRegistryStub> core() {
        return CoreInstanceCreator.createIsolatedCore(workDir.toFile());
    }

    private byte[] fixtureJar() throws Exception {
        byte[] dependency = archive(entry("com/example/dep/Marker.class", plainClass("com.example.dep.Marker")));
        return archive(
                entry(SERVICE_FILE, "com.example.fixture.Fixture\n".getBytes(StandardCharsets.UTF_8)),
                entry("com/example/fixture/Fixture.class", extensionClass("com.example.fixture.Fixture",
                        "public java.lang.String getExtensionName() { return \"fixture\"; }",
                        """
                        public void init(com.jsmacrosce.jsmacros.core.Core runner) {
                            java.lang.String state = "ok";
                            try {
                                java.lang.Class.forName("com.example.dep.Marker");
                            } catch (java.lang.Throwable t) {
                                state = "missing";
                            }
                            com.jsmacrosce.jsmacros.core.extensions.IntegrationProbe.record("fixture", state);
                        }
                        """)),
                entry("jsmacrosce.ext.fixture.json",
                        "{\"dependencies\": [\"META-INF/jsmacroscedeps/dependency.jar\"]}".getBytes(StandardCharsets.UTF_8)),
                entry("META-INF/jsmacroscedeps/dependency.jar", dependency)
        );
    }

    private byte[] dupeJar(String marker) throws Exception {
        return archive(
                entry(SERVICE_FILE, ("com.example.fixture.Dupe" + marker + "\n").getBytes(StandardCharsets.UTF_8)),
                entry("com/example/fixture/Dupe" + marker + ".class", extensionClass("com.example.fixture.Dupe" + marker,
                        "public java.lang.String getExtensionName() { return \"dupe\"; }",
                        """
                        public void init(com.jsmacrosce.jsmacros.core.Core runner) {
                            com.jsmacrosce.jsmacros.core.extensions.IntegrationProbe.record("dupe", "%s");
                        }
                        """.formatted(marker)))
        );
    }

    private byte[] failingJar() throws Exception {
        return archive(
                entry(SERVICE_FILE, "com.example.fixture.Failing\n".getBytes(StandardCharsets.UTF_8)),
                entry("com/example/fixture/Failing.class", extensionClass("com.example.fixture.Failing",
                        "public java.lang.String getExtensionName() { return \"failing\"; }",
                        """
                        public void init(com.jsmacrosce.jsmacros.core.Core runner) {
                            throw new java.lang.RuntimeException("extension init failed on purpose");
                        }
                        """))
        );
    }

    /** Generates a plain class, used for the class the dependency archive contains. */
    private byte[] plainClass(String className) throws Exception {
        return classFile(className, false);
    }

    /** Generates an {@link Extension} implementation, so the loader can discover and cast it. */
    private byte[] extensionClass(String className, String... methods) throws Exception {
        return classFile(className, true, methods);
    }

    private byte[] classFile(String className, boolean extension, String... methods) throws Exception {
        CtClass ct = pool.makeClass(className);
        if (extension) {
            ct.addInterface(pool.get(Extension.class.getName()));
        }
        for (String method : methods) {
            ct.addMethod(CtNewMethod.make(method, ct));
        }
        return ct.toBytecode();
    }

    private void writeExtensionJar(String fileName, byte[] bytes) throws IOException {
        Path extensions = workDir.resolve("Extensions");
        Files.createDirectories(extensions);
        Files.write(extensions.resolve(fileName), bytes);
    }

    private record Entry(String name, byte[] content) {
    }

    private static Entry entry(String name, byte[] content) {
        return new Entry(name, content);
    }

    private static byte[] archive(Entry... entries) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Entry entry : entries) {
                zip.putNextEntry(new ZipEntry(entry.name()));
                zip.write(entry.content());
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }


    private byte[] reentrantJar() throws Exception {
        return archive(
                entry(SERVICE_FILE, "com.example.fixture.Reentrant\n".getBytes(StandardCharsets.UTF_8)),
                entry("com/example/fixture/Reentrant.class", extensionClass("com.example.fixture.Reentrant",
                        "public java.lang.String getExtensionName() { return \"reentrant\"; }",
                        """
                        public void init(com.jsmacrosce.jsmacros.core.Core runner) {
                            runner.extensions.getAllExtensions();
                            com.jsmacrosce.jsmacros.core.extensions.IntegrationProbe.record("reentrant", "ok");
                        }
                        """))
        );
    }

    private byte[] throwingNameJar() throws Exception {
        return archive(
                entry(SERVICE_FILE, "com.example.fixture.Unnamed\n".getBytes(StandardCharsets.UTF_8)),
                entry("com/example/fixture/Unnamed.class", extensionClass("com.example.fixture.Unnamed",
                        "public java.lang.String getExtensionName() { throw new java.lang.RuntimeException(\"name failed\"); }",
                        "public void init(com.jsmacrosce.jsmacros.core.Core runner) {}"))
        );
    }

    private byte[] batchJar() throws Exception {
        byte[] dependency = archive(entry("com/example/dep/Marker.class", plainClass("com.example.dep.Marker")));
        return archive(
                entry(SERVICE_FILE, "com.example.fixture.Batch\n".getBytes(StandardCharsets.UTF_8)),
                entry("com/example/fixture/Batch.class", extensionClass("com.example.fixture.Batch",
                        "public java.lang.String getExtensionName() { return \"batch\"; }",
                        "public void init(com.jsmacrosce.jsmacros.core.Core runner) {}")),
                entry("jsmacrosce.ext.batch.json",
                        "{\"dependencies\": [\"META-INF/jsmacroscedeps/good.jar\", \"META-INF/jsmacroscedeps/bad.jar\"]}"
                                .getBytes(StandardCharsets.UTF_8)),
                entry("META-INF/jsmacroscedeps/good.jar", dependency),
                entry("META-INF/jsmacroscedeps/bad.jar", "not a zip".getBytes(StandardCharsets.UTF_8))
        );
    }

    private byte[] observerJar() throws Exception {
        return archive(
                entry(SERVICE_FILE, "com.example.fixture.Observer\n".getBytes(StandardCharsets.UTF_8)),
                entry("com/example/fixture/Observer.class", extensionClass("com.example.fixture.Observer",
                        "public java.lang.String getExtensionName() { return \"observer\"; }",
                        """
                        public void init(com.jsmacrosce.jsmacros.core.Core runner) {
                            java.lang.String state = "missing";
                            try {
                                java.lang.Class.forName("com.example.dep.Marker");
                                state = "present";
                            } catch (java.lang.Throwable ignored) {
                            }
                            com.jsmacrosce.jsmacros.core.extensions.IntegrationProbe.record("observer", state);
                        }
                        """))
        );
    }

}
