package com.jsmacrosce.jsmacros.test.stubs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.jsmacrosce.jsmacros.core.Core;

import java.io.File;

/**
 * Builds a {@link Core} for tests without a Minecraft client.
 *
 * <p>{@link #createCore()} returns one shared instance rooted at {@code run/config}, which is what
 * the script tests use. {@link #createIsolatedCore(File)} returns a fresh instance rooted at a
 * caller-supplied directory, so a test that installs extensions can have its own extension
 * directory and configuration without disturbing other tests.
 */
public class CoreInstanceCreator {
    private static final Logger LOGGER = LoggerFactory.getLogger("JsMacrosCE");
    private static final File configFolder = new File("run/config");
    private static final File macroFolder = new File(configFolder, "macro");

    /** The shared core used by the script tests. */
    public static Core<ProfileStub, EventRegistryStub> core;

    /**
     * Returns the shared core, creating it on first use.
     *
     * @return the shared core
     */
    public static Core<ProfileStub, EventRegistryStub> createCore() {
        if (core == null) {
            core = createIsolatedCore(configFolder);
        }
        return core;
    }

    /**
     * Creates a core rooted at the supplied directory. Nothing is shared with
     * {@link #createCore()}, so the returned core can load extensions on its own.
     *
     * @param configFolder configuration directory to use; scripts go in its {@code Macros} child
     * @return a new core
     */
    public static Core<ProfileStub, EventRegistryStub> createIsolatedCore(File configFolder) {
        return new Core<>(
                EventRegistryStub::new,
                ProfileStub::new,
                configFolder,
                new File(configFolder, "Macros"),
                LOGGER
        );
    }

}
