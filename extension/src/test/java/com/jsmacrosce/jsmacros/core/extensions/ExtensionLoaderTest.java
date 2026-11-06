package com.jsmacrosce.jsmacros.core.extensions;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.test.extension.LoadedExtension;
import com.jsmacrosce.jsmacros.test.stubs.CoreInstanceCreator;
import com.jsmacrosce.jsmacros.test.stubs.EventRegistryStub;
import com.jsmacrosce.jsmacros.test.stubs.ProfileStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit-level checks for the loader's query surface.
 *
 * <p>Each test builds its own core so that loading and its state are not shared between tests.
 * The provider these tests see comes from this module's test classpath; archive discovery,
 * ordering, and failure isolation are covered by {@link ExtensionLoaderIntegrationTest}.
 */
class ExtensionLoaderTest {

    private final Path workDir = Path.of("build", "test-extension-loader", UUID.randomUUID().toString());

    @BeforeEach
    void resetLoadedFlag() {
        LoadedExtension.initialized = false;
    }

    @Test
    void loadsTheExtensionsItCanRead() throws IOException {
        Core<ProfileStub, EventRegistryStub> core = core();

        Extension loaded = core.extensions.getExtensionForName("loaded");

        assertNotNull(loaded, "the extension named in the service file should be loaded");
        assertTrue(LoadedExtension.initialized, "the extension should have been initialized");
    }

    @Test
    void reportsNotLoadedUntilTheFirstQuery() throws IOException {
        Core<ProfileStub, EventRegistryStub> core = core();

        assertTrue(core.extensions.notLoaded(), "the loader should not load extensions during construction");
        core.extensions.getAllExtensions();
        assertFalse(core.extensions.notLoaded(), "the first query should have completed loading");
    }

    @Test
    void matchesExtensionsByExactName() throws IOException {
        Core<ProfileStub, EventRegistryStub> core = core();

        assertTrue(core.extensions.isExtensionLoaded("loaded"));
        assertFalse(core.extensions.isExtensionLoaded("Loaded"),
                "extension names should be matched exactly, not case-insensitively");
    }

    private Core<ProfileStub, EventRegistryStub> core() throws IOException {
        Files.createDirectories(workDir);
        return CoreInstanceCreator.createIsolatedCore(workDir.toFile());
    }

}
