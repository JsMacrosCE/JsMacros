package com.jsmacrosce.jsmacros.api.helper;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.List;

/**
 * A mod that is loaded, wrapped so a script can ask what it is. There is no way to make one of
 * these: they come from
 * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FClient#getMod(java.lang.String) getMod()}
 * for a single mod, or from
 * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FClient#getLoadedMods() getLoadedMods()}
 * for every one of them.<br>
 * What the wrapper reads comes from the mod's own metadata, which its author wrote, so most of it
 * is free text and the only field worth matching against is {@link #getId() getId()}. What a
 * loader fills in also varies: {@link #getAuthors() getAuthors()} is a list on both loaders, but
 * the NeoForge one hands back {@code null} rather than filling it in, and
 * {@link #getEnv() getEnv()} can come back as {@code 'UNKNOWN'} when the mod's own configuration
 * does not say which side it is for. Check {@link #getEnv() getEnv()} against the three known
 * strings before relying on it, and treat a {@code null} author list as "not reported" rather than
 * "no authors".<br>
 * The underlying loader object is still reachable through
 * {@link com.jsmacrosce.jsmacros.core.helpers.BaseHelper#getRaw() getRaw()} for anything this
 * wrapper does not cover.
 * example:
 * <pre>
 * // everything that is loaded, id and version only
 * const mods = Client.getLoadedMods();
 * let i = 0;
 * while (i !== mods.size()) {
 *   Chat.log(`${mods.get(i).getId()} ${mods.get(i).getVersion()}`);
 *   i += 1;
 * }
 *
 * // one mod by id, with the metadata it declares about itself
 * const sodium = Client.getMod("sodium");
 * if (sodium !== null) {
 *   // getAuthors() is null on NeoForge, so name the side it came from
 *   const authors = sodium.getAuthors();
 *   const by = authors === null ? "an unreported author" : authors;
 *   Chat.log(`${sodium.getName()} by ${by}, runs on ${sodium.getEnv()}`);
 * }
 * </pre>
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
     * the mod's id, which is the bare name a mod calls itself rather than the
     * {@code namespace:name} form other APIs here use for ids. That is {@code 'sodium'}, not
     * {@code 'minecraft:sodium'}, since a mod lives in its own namespace and this is the name
     * rather than a namespaced identifier. It is also the value
     * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FClient#getMod(java.lang.String) getMod()}
     * looks a mod up by, so the id is the way to get from one of these wrappers back to a
     * specific mod.
     *
     * @return the mod's id.
     * @since 1.8.4
     */
    public abstract String getId();

    /**
     * the mod's display name, which is what it calls itself and need not be its id. A mod is free
     * to pick anything here, and different builds of the same mod can report different names, so
     * match on {@link #getId() getId()} rather than on this when looking one up.
     *
     * @return the mod's name.
     * @since 1.8.4
     */
    public abstract String getName();

    /**
     * the mod's description, the long text its author wrote for the mod list. It is plain text
     * with formatting codes in it on some loaders, and there is no reason to expect a script to
     * match against it.
     *
     * @return the mod's description.
     * @since 1.8.4
     */
    public abstract String getDescription();

    /**
     * the mod's version, as the string its author wrote rather than anything comparable. It is not
     * a number and cannot be ordered, so two versions of the same mod can only be compared for
     * equality, and different mods can report versions in incompatible formats.
     *
     * @return the mod's version.
     * @since 1.8.4
     */
    public abstract String getVersion();

    /**
     * which side the mod is meant to run on: {@code 'CLIENT'}, {@code 'SERVER'} or {@code 'BOTH'}.
     * A mod that is running on this side of the game may still report a side that is not this
     * one, since the two are not the same question.<br>
     * This can also come back as {@code 'UNKNOWN'}, which is the NeoForge loader's answer when the
     * mod's own configuration does not name a side. The Fabric loader only ever reports the three
     * named sides and throws rather than producing anything else, so check the value before
     * treating it as one of them.
     *
     * @return the environment this mod is intended for.
     * @since 1.8.4
     */
    public abstract String getEnv();

    /**
     * the names on the mod's credits, in the order the mod lists them.<br>
     * On NeoForge this is {@code null} rather than a list, because that loader does not hand the
     * information over, so check the result against {@code null} before reading it. The Fabric
     * loader reports what the mod's own metadata declares, which is whatever its author typed and
     * can be empty.
     *
     * @return a list of all authors, or {@code null} on NeoForge where it is not reported
     * @since 1.8.4
     */
    public abstract List<String> getAuthors();

    /**
     * the ids the mod listed as its dependencies, taken straight from its own metadata with no
     * filtering, so this is everything its author declared rather than everything that has to be
     * present for the mod to run. On Fabric that is every entry in the mod's dependency table,
     * which means it also includes the {@code conflicts} and {@code breaks} entries alongside the
     * required, recommended and suggested ones, and an entry for something that is not a mod at
     * all is listed the same way as one that is, so ids such as {@code minecraft}, {@code java}
     * or {@code fabricloader} can turn up here. On NeoForge it is the same in kind: every entry
     * in the dependency table the mod's own configuration declares, mapped to the mod id each one
     * names. Either way this is the author's list and not a resolved one, so a game version or a
     * library that no mod provides will not appear, and a missing id means "not declared" rather
     * than "not required".<br>
     * An empty list means the mod declared no dependencies, not that it needs none.
     *
     * @return a list of all dependencies.
     * @since 1.8.4
     */
    public abstract List<String> getDependencies();

    @Override
    public String toString() {
        return String.format("ModContainerHelper:{\"id\": \"%s\", \"name\": \"%s\", \"version\": \"%s\"}", getId(), getName(), getVersion());
    }

}
