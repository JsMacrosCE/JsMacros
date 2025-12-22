package xyz.wagyourtail.jsmacros.core.config;

import com.google.common.io.Files;
import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.slf4j.Logger;
import xyz.wagyourtail.jsmacros.core.Core;

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
    public final Map<String, Class<?>> optionClasses = new LinkedHashMap<>();
    public final Map<Class<?>, Object> options = new LinkedHashMap<>();
    public final File configFolder;
    public final File macroFolder;
    public final File configFile;
    public final Logger LOGGER;
    public final static int TARGET_VERSION = 4;
    int loadedAsVers = TARGET_VERSION;
    public JsonObject rawOptions = null;

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

    @SuppressWarnings("unchecked")
    public <T> T getOptions(Class<T> optionClass) {
        if (!options.containsKey(optionClass)) {
            return null;
        }
        return (T) options.get(optionClass);
    }

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

    public synchronized void backupConfig() throws IOException {
        final File back = new File(configFolder, "options.json.v" + loadedAsVers + ".bak");
        if (back.exists()) {
            back.delete();
        }
        Files.move(configFile, back);
        saveConfig();
    }

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

    public void loadDefaults() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        for (Map.Entry<String, Class<?>> optionClass : optionClasses.entrySet()) {
            options.put(optionClass.getValue(), optionClass.getValue().getConstructor().newInstance());
            rawOptions = new JsonObject();
            rawOptions.addProperty("version", TARGET_VERSION);
        }
    }

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
