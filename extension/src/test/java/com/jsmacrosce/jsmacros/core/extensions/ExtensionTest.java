package com.jsmacrosce.jsmacros.core.extensions;

import com.jsmacrosce.jsmacros.core.Core;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ResourceLock(Resources.SYSTEM_ERR)
class ExtensionTest {

    /** Extension whose translations live under the {@code jsmacrosce} namespace. */
    public static class NamespacedExtension implements Extension {
        @Override
        public String getExtensionName() {
            return "testext";
        }

        @Override
        public void init(Core<?, ?> runner) {
        }
    }

    /** Extension whose translations still use the unsupported {@code jsmacros} namespace. */
    public static class LegacyExtension implements Extension {
        @Override
        public String getExtensionName() {
            return "testlegacy";
        }

        @Override
        public void init(Core<?, ?> runner) {
        }
    }

    /** Extension that ships no dependency descriptor at all. */
    public static class BareExtension implements Extension {
        @Override
        public String getExtensionName() {
            return "testbare";
        }

        @Override
        public void init(Core<?, ?> runner) {
        }
    }

    @Test
    void readsOnlyNamespacedTranslationsEvenWhenLegacyResourcesExist() {
        String warnings = captureWarnings(() -> {
            Map<String, String> translations = new NamespacedExtension().getTranslations("en_us");
            assertEquals(Map.of("testext.key", "value"), translations);
        });
        assertTrue(warnings.isEmpty(), "current translations should not trigger a migration warning");
    }

    @Test
    void ignoresLegacyTranslationsAndReportsTheMigrationPath() {
        String warnings = captureWarnings(() ->
                assertTrue(new LegacyExtension().getTranslations("en_us").isEmpty()));

        assertTrue(warnings.contains("testlegacy"), "the warning should identify the extension");
        assertTrue(warnings.contains("assets/jsmacros/testlegacy/lang/en_us.json"));
        assertTrue(warnings.contains("assets/jsmacrosce/testlegacy/lang/en_us.json"));
        assertTrue(warnings.contains("ignored"), "the warning should explain that translations were not loaded");
    }

    @Test
    void reportsNoTranslationsForAnUnknownLanguage() {
        String warnings = captureWarnings(() ->
                assertTrue(new NamespacedExtension().getTranslations("xx_xx").isEmpty()));
        assertTrue(warnings.isEmpty(), "optional translations should not warn when neither path exists");
    }

    private static String captureWarnings(Runnable action) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream previous = System.err;
        try (PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            System.setErr(capture);
            action.run();
        } finally {
            System.setErr(previous);
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    @Test
    void reportsNoDependenciesWhenTheDescriptorIsMissing() {
        Set<URL> dependencies = new BareExtension().getDependencies();
        assertTrue(dependencies.isEmpty());
    }

    @Test
    void findsTheDependencyDescriptorOfAnExtensionThatHasOne() {
        assertFalse(new DependencyExtension().getDependencies().isEmpty());
    }

    @Test
    void keepsDependenciesInTheOrderTheDescriptorListsThem() {
        List<URL> dependencies = new ArrayList<>(
                Extension.getDependenciesInternal(ExtensionTest.class, "jsmacrosce.ext.ordered.json"));

        assertEquals(2, dependencies.size());
        assertTrue(dependencies.get(0).getPath().endsWith("jsmacrosce.ext.testdep.json"),
                "the first listed dependency should come first, not the alphabetically first resource");
        assertTrue(dependencies.get(1).getPath().endsWith("assets/jsmacrosce/testext/lang/en_us.json"));
    }

    @Test
    void rejectsTheSpaceSeparatedDependencyForm() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> Extension.getDependenciesInternal(ExtensionTest.class, "jsmacrosce.ext.primitive.json"));
        assertTrue(error.getMessage().contains("JSON array"),
                "the error should say what the descriptor must look like: " + error.getMessage());
    }

    @Test
    void skipsADeclaredDependencyThatIsNotOnTheClasspath() {
        // The descriptor is still valid; only the archive is absent, which a flat classpath can
        // legitimately cause. The entry is dropped and reported, not fatal.
        Set<URL> dependencies = Extension.getDependenciesInternal(ExtensionTest.class, "jsmacrosce.ext.missing.json");
        assertTrue(dependencies.isEmpty());
    }

    @Test
    void rejectsADescriptorThatIsNotValidJson() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> Extension.getDependenciesInternal(ExtensionTest.class, "jsmacrosce.ext.broken.json"));
        assertTrue(error.getMessage().contains("jsmacrosce.ext.broken.json"), error.getMessage());
    }

    /** Extension that declares a dependency through its own descriptor. */
    public static class DependencyExtension implements Extension {
        @Override
        public String getExtensionName() {
            return "testdep";
        }

        @Override
        public void init(Core<?, ?> runner) {
        }
    }

}
