package com.jsmacrosce.jsmacros.client;

import com.jsmacrosce.jsmacros.api.helper.ModContainerHelper;

import java.util.List;

/**
 * Provides mod-loader metadata and lookup operations to the client API.
 * @author Etheradon
 * @since 1.8.4
 */
public interface ModLoader {

    /**
     * Reports whether the mod loader identifies the current environment as a development environment.
     *
     * @return {@code true} when the mod loader reports a development environment
     * @since 1.8.4
     */
    boolean isDevEnv();

    /**
     * Returns the name of this mod loader.
     * @return this mod loader's name
     * @since 1.8.4
     */
    String getName();

    /**
     * Returns the mod metadata entries reported as loaded by this mod loader.
     * @return the loaded mod metadata entries
     * @since 1.8.4
     */
    List<? extends ModContainerHelper<?>> getLoadedMods();

    /**
     * Reports whether this mod loader considers the specified mod ID loaded.
     *
     * @param modId the mod id to check
     * @return {@code true} when the mod loader reports the ID as loaded
     * @since 1.8.4
     */
    boolean isModLoaded(String modId);

    /**
     * Looks up a mod by ID in this mod loader.
     *
     * @param modId the mod id
     * @return the mod container for the ID, or {@code null} if this mod loader has no matching mod.
     * @since 1.8.4
     */
    ModContainerHelper<?> getMod(String modId);

}
