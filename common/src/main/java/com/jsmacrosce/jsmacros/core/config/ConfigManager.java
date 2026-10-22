package com.jsmacrosce.jsmacros.core.config;

import com.google.common.io.Files;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.Core;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manages registered configuration-option classes and the JSON data loaded for them.
 * Scripts can obtain the manager from {@code JsMacros.getConfig()}:
 * <pre>
 * const config = JsMacros.getConfig();
 * Chat.log(config.configFile.getPath());
 * </pre>
 */
@DocletCategory("Configuration/Profiles")
public class ConfigManager {
    protected final static Gson gson = new GsonBuilder()
        .registerTypeAdapter(File.class, new TypeAdapter<File>() {
            @Override
            public void write(JsonWriter jsonWriter, File file) throws IOException {
                jsonWriter.value(file.getPath());
            }

            @Override
            public File read(JsonReader jsonReader) throws IOException {
                return new File(jsonReader.nextString());
            }
        })
        .registerTypeHierarchyAdapter(Path.class, new TypeAdapter<Path>() {

            @Override
            public void write(JsonWriter jsonWriter, Path path) throws IOException {
                jsonWriter.value(path.toString());
            }

            @Override
            public Path read(JsonReader jsonReader) throws IOException {
                return Path.of(jsonReader.nextString());
            }

        }).setPrettyPrinting().create();

    private final Core<?, ?> runner;
    /** Maps JSON root property names to registered option classes. */
    public final Map<String, Class<?>> optionClasses = new LinkedHashMap<>();
    /** Maps registered option classes to their loaded or default option values. */
    public final Map<Class<?>, Object> options = new LinkedHashMap<>();
    /** Absolute configuration directory retained by this manager. */
    public final File configFolder;
    /** Absolute macro directory retained by this manager. */
    public final File macroFolder;
    /** Configuration JSON file, named {@code options.json} under the constructor's configuration-directory argument. */
    public final File configFile;
    /** Logger used for configuration operations. */
    public final Logger LOGGER;
    /** Schema version written to configuration data; this is not the JsMacros release version. */
    public final static int TARGET_VERSION = 4;
    int loadedAsVers = TARGET_VERSION;
    /** Raw JSON data most recently read or built; initially {@code null} and not automatically synchronized with {@code options}. */
    public JsonObject rawOptions = null;

    /** Initializes the core option registration and loads configuration, attempting to create the supplied directories when missing.
     * If the macro directory is initially missing, also attempts to create its {@code index.js}; directory and file creation are best-effort.
     * Checked failures from configuration loading are wrapped in {@code RuntimeException}.
     * @param runner runtime retained for option setup
     * @param configFolder directory used to locate the options file
     * @param macroFolder directory for macros
     * @param logger logger for configuration operations
     */
    public ConfigManager(Core<?, ?> runner, File configFolder, File macroFolder, Logger logger) {
        this.runner = runner;
        this.configFolder = configFolder.getAbsoluteFile();
        this.macroFolder = macroFolder.getAbsoluteFile();
        this.LOGGER = logger;
        if (!configFolder.exists()) {
            configFolder.mkdirs();
        }
        this.configFile = new File(configFolder, "options.json");
        if (!macroFolder.exists()) {
            macroFolder.mkdirs();
            final File tf = new File(macroFolder, "index.js");
            if (!tf.exists()) {
                try {
                    tf.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        optionClasses.put("core", CoreConfigV2.class);

        try {
            loadConfig();
        } catch (IllegalAccessException | InstantiationException | IOException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /** Reads raw JSON and its schema version without rebuilding typed option values; if the file is missing, builds registered defaults.
     * A missing version is treated as {@code 1}. Read, parse, and default-construction failures propagate.
     */
    public synchronized void reloadRawConfigFromFile() throws IOException, InvocationTargetException, IllegalAccessException, InstantiationException, NoSuchMethodException {
        if (!configFile.exists()) {
            loadDefaults();
            return;
        }
        try (FileReader reader = new FileReader(configFile)) {
            rawOptions = new JsonParser().parse(reader).getAsJsonObject();
            JsonElement version = rawOptions.get("version");
            loadedAsVers = version == null ? 1 : version.getAsInt();
        }
    }

    protected synchronized void convertOrLoadConfigs() throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException, IOException {
        for (Map.Entry<String, Class<?>> optionClass : optionClasses.entrySet()) {
            try {
                convertOrLoadConfig(optionClass.getKey(), optionClass.getValue());
            } catch (Exception e) {
                backupConfig();
                LOGGER.error("Failed to load option " + optionClass.getKey(), e);
                options.put(optionClass.getValue(), optionClass.getValue().getConstructor().newInstance());
            }
            if (options.get(optionClass.getValue()) == null) {
                options.put(optionClass.getValue(), optionClass.getValue().getConstructor().newInstance());
            }
            try {
                Field f = optionClass.getValue().getDeclaredField("runner");
                f.setAccessible(true);
                f.set(options.get(optionClass.getValue()), runner);
            } catch (NoSuchFieldException ignored) {}
        }
        rawOptions.addProperty("version", TARGET_VERSION);
    }

    protected synchronized void convertOrLoadConfig(String key, Class<?> clazz) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        try {
            Method m = clazz.getDeclaredMethod("fromV" + loadedAsVers, JsonObject.class);
            Object option = clazz.getDeclaredConstructor().newInstance();
            m.invoke(option, rawOptions);
            options.put(clazz, option);
        } catch (NoSuchMethodException ignored) {
            options.put(clazz, gson.fromJson(rawOptions.get(key), clazz));
        }
    }

    /** Returns the stored option value for a registered class; does not instantiate or reload it.
     * @param <T> option value type
     * @param optionClass class used as the option-value key
     * @return stored value, or {@code null} if absent or stored as {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> T getOptions(Class<T> optionClass) {
        if (!options.containsKey(optionClass)) {
            return null;
        }
        return (T) options.get(optionClass);
    }

    /**
     * Registers an option class and loads its value, falling back to a new default instance if loading fails.
     * Registration occurs before loading and is retained if loading fails. Only a duplicate JSON key is rejected; the same class may be registered under another key, replacing its value in {@code options}.
     * A declared {@code runner} field is injected when present. Default construction requires an accessible public no-argument constructor.
     * @param key JSON root property name; must not already be registered
     * @param optionClass option class to register and load
     */
    public synchronized void addOptions(String key, Class<?> optionClass) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, IOException {
        if (optionClasses.containsKey(key)) {
            throw new IllegalStateException("Key \"" + key + "\" already in config manager!");
        }
        optionClasses.put(key, optionClass);
        try {
            convertOrLoadConfig(key, optionClass);
        } catch (Exception ex) {
            backupConfig();
            LOGGER.error("Failed to load option " + key, ex);
            options.put(optionClass, optionClass.getConstructor().newInstance());
            saveConfig();
        }
        if (options.get(optionClass) == null) {
            options.put(optionClass, optionClass.getConstructor().newInstance());
        }
        try {
            Field f = optionClass.getDeclaredField("runner");
            f.setAccessible(true);
            f.set(options.get(optionClass), runner);
        } catch (NoSuchFieldException ignored) {}

        // If we loaded from an older version and converted this option, persist the migrated config
        if (loadedAsVers != TARGET_VERSION) {
            try {
                if (configFile.exists()) {
                    backupConfig();
                }
                saveConfig();
            } catch (IOException e) {
                LOGGER.error("Failed to persist migrated config for option " + key, e);
            }
        }
    }

    /**
     * Moves the current file to {@code options.json.vN.bak}, where N is the loaded config version, then attempts to save current state. Moving can fail with IOException; saveConfig catches save failures.
     */
    public synchronized void backupConfig() throws IOException {
        final File back = new File(configFolder, "options.json.v" + loadedAsVers + ".bak");
        if (back.exists()) {
            back.delete();
        }
        Files.move(configFile, back);
        saveConfig();
    }

    /**
     * Clears existing instances and reloads them from disk, converting older data when supported. Loading failures caught per option fall back to that option's defaults; an InvocationTargetException or NoSuchMethodException escaping conversion or default construction instead resets all registered options to defaults. If no method named {@code fromV} followed by the loaded version exists, Gson loads the option instead of using the outer fallback. Backs up when the loaded version differs.
     */
    public synchronized void loadConfig() throws IllegalAccessException, InstantiationException, IOException, InvocationTargetException, NoSuchMethodException {
        options.clear();
        reloadRawConfigFromFile();
        try {
            convertOrLoadConfigs();
        } catch (InvocationTargetException | NoSuchMethodException e) {
            LOGGER.error("Failed to load config", e);
            loadDefaults();
        } finally {
            if (loadedAsVers != TARGET_VERSION) {
                backupConfig();
            }
        }
        LOGGER.info("Loaded Profiles:");
        for (String key : getOptions(CoreConfigV2.class).profileOptions()) {
            LOGGER.info("    " + key);
        }

    }

    /**
     * Instantiates defaults for registered option classes and resets raw data to an empty current-version JSON object.
     * Does not inject runner fields or write the config file.
     */
    public void loadDefaults() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        for (Map.Entry<String, Class<?>> optionClass : optionClasses.entrySet()) {
            options.put(optionClass.getValue(), optionClass.getValue().getConstructor().newInstance());
            rawOptions = new JsonObject();
            rawOptions.addProperty("version", TARGET_VERSION);
        }
    }

    /**
     * Attempts to serialize registered options into the raw JSON data and write it to the config file.
     * Exceptions during serialization or writing are logged and not propagated.
     */
    public void saveConfig() {
        try {
            for (Map.Entry<String, Class<?>> optionClass : optionClasses.entrySet()) {
                rawOptions.add(optionClass.getKey(), gson.toJsonTree(options.get(optionClass.getValue())));
            }
            final FileWriter fw = new FileWriter(configFile);
            fw.write(gson.toJson(rawOptions));
            fw.close();
        } catch (Exception e) {
            LOGGER.error("Config Failed To Save.");
            e.printStackTrace();
        }
    }

}
