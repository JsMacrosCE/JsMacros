package com.jsmacrosce.jsmacros.core.extensions;


import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jsmacrosce.jsmacros.core.Core;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
     * Missing dependency entries are reported to standard error and skipped; descriptor read or
     * parse failures are propagated as runtime exceptions.
     *
     * @return resolved dependency resource URLs; empty when no entries resolve or the descriptor
     *         has no dependencies or the literal {@code ${dependencies}} placeholder
     */
    default Set<URL> getDependencies() {
        return getDependenciesInternal(this.getClass(), "jsmacrosce.ext." + getExtensionName() + ".json");
    }

    /**
     * Resolves dependency URLs from a root-classpath JSON descriptor. A primitive
     * {@code dependencies} value is split on spaces and each entry is prefixed with
     * {@code META-INF/jsmacrosdeps/}; an array supplies root-relative resource paths directly.
     * The exact {@code ${dependencies}} primitive is treated as empty. Unresolved resources are
     * reported to standard error and skipped. Descriptor read or parse failures and unsupported
     * dependency formats cause runtime exceptions; other malformed data or value-conversion
     * failures can also cause runtime exceptions.
     *
     * @param clazz class used to look up the descriptor and its dependency resources
     * @param fname descriptor path relative to the classpath root, without a leading slash
     * @return resolved dependency resource URLs, or an empty set when dependencies are absent or
     *         the placeholder is used
     */
    static Set<URL> getDependenciesInternal(Class<?> clazz, String fname) {
        JsonElement json;
        try (Reader reader = new InputStreamReader(clazz.getResourceAsStream("/" + fname))) {
            json = new JsonParser().parse(reader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        JsonElement dependencies = json.getAsJsonObject().get("dependencies");
        if (dependencies == null) {
            return new HashSet<>();
        }
        String[] dependenciesArray;
        if (dependencies.isJsonPrimitive()) {
            String dependenciesString = dependencies.getAsString();
            if (dependenciesString.equals("${dependencies}")) {
                return new HashSet<>();
            }
            dependenciesArray = dependenciesString.split(" ");
            for (int i = 0; i < dependenciesArray.length; i++) {
                dependenciesArray[i] = "META-INF/jsmacrosdeps/" + dependenciesArray[i].trim();
            }
        } else if (dependencies.isJsonArray()) {
            dependenciesArray = new String[dependencies.getAsJsonArray().size()];
            for (int i = 0; i < dependenciesArray.length; i++) {
                dependenciesArray[i] = dependencies.getAsJsonArray().get(i).getAsString();
            }
        } else {
            throw new RuntimeException("Invalid dependencies format");
        }
        Set<URL> dependenciesSet = new HashSet<>();
        for (String dependency : dependenciesArray) {
            URL resource = clazz.getResource("/" + dependency);
            if (resource != null) {
                dependenciesSet.add(resource);
            } else {
                System.err.println("[JsMacrosExtensionManager] Could not find dependency: " + dependency);
            }
        }
        return dependenciesSet;
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
