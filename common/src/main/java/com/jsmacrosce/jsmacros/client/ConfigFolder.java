package com.jsmacrosce.jsmacros.client;

import java.io.File;

/**
 * Service for locating the JsMacros configuration directory. JsMacros loads a
 * provider through {@link java.util.ServiceLoader}; the bundled Fabric and
 * NeoForge providers return a child named {@code jsMacros}.
 * <pre>
 * const ConfigFolder = Java.type("com.jsmacrosce.jsmacros.client.ConfigFolder");
 * const ServiceLoader = Java.type("java.util.ServiceLoader");
 * const provider = ServiceLoader.load(ConfigFolder.class).findFirst();
 * if (provider.isPresent()) Chat.log(provider.get().getFolder());
 * </pre>
 */
public interface ConfigFolder {
    /**
     * Returns the provider's JsMacros configuration directory.
     * @return the configured directory
     */
    File getFolder();

}
