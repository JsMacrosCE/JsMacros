package com.jsmacrosce.jsmacros.api.helper;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.List;

/**
 * A loaded mod's metadata exposed to scripts. Obtain wrappers from
 * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FClient#getMod(java.lang.String) getMod()}
 * for a single mod, or from
 * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FClient#getLoadedMods() getLoadedMods()}.
 * Values come from loader metadata; use {@link #getId() getId()} to identify a mod rather than
 * its display name. Loader differences include {@link #getAuthors()} returning {@code null} and
 * {@link #getEnv()} returning {@code "UNKNOWN"}.
 * <pre>
 * // List loaded mods and their ids and versions
 * const mods = Client.getLoadedMods();
 * let i = 0;
 * while (i !== mods.size()) {
 *   Chat.log(`${mods.get(i).getId()} ${mods.get(i).getVersion()}`);
 *   i += 1;
 * }
 *
 * // Look up a mod by id
 * const sodium = Client.getMod("sodium");
 * if (sodium !== null) {
 *   Chat.log(sodium.getName());
 * }
 * </pre>
 * @param <T> the loader-specific mod container wrapped by this helper
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public abstract class ModContainerHelper<T> extends BaseHelper<T> {

    protected ModContainerHelper(T base) {
        super(base);
    }

    /**
     * Returns the mod's loader-defined id (for example, {@code "sodium"}), used to look it up with
     * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FClient#getMod(java.lang.String) getMod()}.
     *
     * @return the loader-defined id used by {@code Client.getMod(id)}
     * @since 1.8.4
     */
    public abstract String getId();

    /**
     * Returns the mod's display name; it need not match {@link #getId()}.
     *
     * @return the mod's name.
     * @since 1.8.4
     */
    public abstract String getName();

    /**
     * Returns the mod's description from loader metadata.
     *
     * @return the mod's description.
     * @since 1.8.4
     */
    public abstract String getDescription();

    /**
     * Returns the mod's loader-reported version string; its format is loader- and mod-defined.
     *
     * @return the mod's version.
     * @since 1.8.4
     */
    public abstract String getVersion();

    /**
     * Returns the mod's declared environment: {@code "CLIENT"}, {@code "SERVER"}, or
     * {@code "BOTH"}; NeoForge may also return {@code "UNKNOWN"} for an absent or unrecognized
     * side value.
     *
     * @return the environment this mod is intended for.
     * @since 1.8.4
     */
    public abstract String getEnv();

    /**
     * Returns the author names from metadata on Fabric; NeoForge returns {@code null}.
     *
     * @return a list of all authors, or {@code null} on NeoForge where it is not reported
     * @since 1.8.4
     */
    public abstract List<String> getAuthors();

    /**
     * Returns ids from the mod's declared dependency metadata, including non-required entries
     * exposed by the loader; this is not a list of resolved required mods.
     *
     * @return a list of all dependencies.
     * @since 1.8.4
     */
    public abstract List<String> getDependencies();

    /**
     * Returns a diagnostic label containing the values from {@link #getId()}, {@link #getName()},
     * and {@link #getVersion()} at call time. The values are inserted with {@code %s} formatting
     * and are not JSON-escaped.
     *
     * @return a label with the mod id, name, and version
     */
    @Override
    public String toString() {
        return String.format("ModContainerHelper:{\"id\": \"%s\", \"name\": \"%s\", \"version\": \"%s\"}", getId(), getName(), getVersion());
    }

}
