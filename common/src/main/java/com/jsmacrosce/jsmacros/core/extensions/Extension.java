package com.jsmacrosce.jsmacros.core.extensions;


import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jsmacrosce.jsmacros.core.Core;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides an extension's identity, dependencies, initialization hook, and optional translations.
 * Extensions are discovered by the loader as service providers. Scripts can enumerate loaded
 * extensions through {@code context.getCtx().runner.extensions.getAllExtensions()}:
 * <pre>
 * const extensions = context.getCtx().runner.extensions.getAllExtensions().iterator();
 * while (extensions.hasNext()) {
 *   Chat.log(extensions.next().getExtensionName());
 * }
 * </pre>
 */
public interface Extension {

    /** Returns the extension identifier used by the loader and default resource paths.
     *
     * @return the extension identifier
     */
    String getExtensionName();

    /** Called by the loader after dependencies are loaded and before this extension's libraries are registered.
     *
     * @param runner core instance supplied by the loader
     */
    void init(Core<?, ?> runner);

    /**
     * Reads this extension's dependency descriptor, {@code jsmacrosce.ext.<name>.json}.
     * A missing descriptor means the extension declares no dependencies.
     *
     * @return resolved dependency resource URLs in the order the descriptor lists them; empty when
     *         the descriptor has no dependencies or none of them resolve
     * @throws IllegalStateException if the descriptor exists but cannot be read, is not valid
     *                               JSON, or does not have the expected shape
     */
    default Set<URL> getDependencies() {
        return getDependenciesInternal(this.getClass(), "jsmacrosce.ext." + getExtensionName() + ".json");
    }

    /**
     * Resolves dependency URLs from a root-classpath JSON descriptor.
     * <p>
     * The descriptor is a JSON object whose {@code dependencies} value is an array of resource
     * paths relative to the classpath root, for example
     * {@code {"dependencies": ["META-INF/jsmacroscedeps/graal-sdk.jar"]}}. Each path names an
     * archive inside the extension's own jar. Entries resolve in the order listed, which is the
     * order the loader hands them to the class loader, so the first archive to contain a class
     * wins.
     * <p>
     * A missing descriptor and an absent {@code dependencies} member both mean "no dependencies".
     * An unreadable, unparsable, or wrongly shaped descriptor throws, because a declaration that
     * cannot be understood must not be mistaken for "no dependencies".
     * <p>
     * A declared path that the classpath does not provide is reported and skipped instead of
     * failing the extension. The descriptor lists archives that are embedded in the extension's
     * own jar, and a development run or other flat classpath may supply the same classes without
     * that archive existing at the declared path. The report names the descriptor and the path, so
     * a dependency that really is missing can still be traced from the warning.
     *
     * @param clazz class used to look up the descriptor and its dependency resources
     * @param fname descriptor path relative to the classpath root, without a leading slash
     * @return resolved dependency resource URLs, in descriptor order
     * @throws IllegalStateException if the descriptor cannot be read or is malformed
     */
    static Set<URL> getDependenciesInternal(Class<?> clazz, String fname) {
        JsonElement json;
        try (InputStream stream = clazz.getResourceAsStream("/" + fname)) {
            if (stream == null) {
                return new LinkedHashSet<>();
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                // A descriptor is a declaration, not user data: parse failures must not be
                // mistaken for "no dependencies".
                json = JsonParser.parseReader(reader);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not read extension descriptor " + fname, e);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Could not parse extension descriptor " + fname, e);
        }

        if (!json.isJsonObject()) {
            throw new IllegalStateException("Extension descriptor " + fname + " must be a JSON object");
        }
        JsonElement dependencies = json.getAsJsonObject().get("dependencies");
        if (dependencies == null || dependencies.isJsonNull()) {
            return new LinkedHashSet<>();
        }
        if (!dependencies.isJsonArray()) {
            throw new IllegalStateException("Extension descriptor " + fname
                    + " must declare dependencies as a JSON array of classpath paths");
        }

        Set<URL> resolved = new LinkedHashSet<>();
        for (JsonElement entry : dependencies.getAsJsonArray()) {
            if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
                throw new IllegalStateException("Extension descriptor " + fname
                        + " has a dependencies entry that is not a string");
            }
            String dependency = entry.getAsString().trim();
            if (dependency.isEmpty()) {
                continue;
            }
            URL resource = clazz.getResource("/" + dependency);
            if (resource == null) {
                System.err.println("[JsMacrosExtensionManager] Extension descriptor " + fname
                        + " declares dependency " + dependency
                        + ", which is not on the classpath; skipping it");
                continue;
            }
            // De-duplicate while keeping declaration order: the loader's first-archive-wins
            // class lookup depends on it.
            resolved.add(resource);
        }
        return resolved;
    }

    /** Reads this extension's translations from its {@code assets/<namespace>/<name>/lang/} resource directory.
     * <p>
     * The namespaces are searched in order, and the first one that holds the requested language
     * file wins. An extension that keeps its translations somewhere else overrides this method.
     *
     * @param lang language-code filename component, such as {@code en_us}
     * @return translation values keyed by their JSON object keys; empty if no language file is
     *         present, if reading or parsing fails, or if the JSON root is not an object. Values
     *         that cannot be converted to strings can cause a runtime exception.
     */
    default Map<String, String> getTranslations(String lang) {
        for (String namespace : translationNamespaces()) {
            String path = "assets/" + namespace + "/" + getExtensionName() + "/lang/" + lang + ".json";
            if (this.getClass().getResource("/" + path) != null) {
                return getTranslationsInternal(this.getClass(), path);
            }
        }
        return Collections.emptyMap();
    }

    /**
     * Returns the resource namespaces an extension's translations may live in, in search order.
     *
     * @return the namespaces to search
     */
    private static List<String> translationNamespaces() {
        return List.of("jsmacros", "jsmacrosce");
    }

    /**
     * Reads a root-classpath JSON object as a map of string values. Read or parse failures and
     * non-object roots produce an empty map.
     *
     * @param clazz class used to look up the resource
     * @param fname resource path relative to the classpath root, without a leading slash
     * @return object entries converted to string values, or an empty map on read or parse failure
     *         or a non-object root; values that cannot be converted can cause a runtime exception
     */
    static Map<String, String> getTranslationsInternal(Class<?> clazz, String fname) {
        JsonElement json;
        try (InputStream stream = clazz.getResourceAsStream("/" + fname)) {
            if (stream == null) {
                return new HashMap<>();
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                json = JsonParser.parseReader(reader);
            }
        } catch (Exception e) {
            // Translations are optional: a broken language file must not stop the extension or
            // the client's own language loading.
            return new HashMap<>();
        }

        if (json.isJsonObject()) {
            return json.getAsJsonObject().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().getAsString()));
        }
        return new HashMap<>();
    }

}
