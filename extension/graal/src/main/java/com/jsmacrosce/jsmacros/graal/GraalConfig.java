package com.jsmacrosce.jsmacros.graal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jsmacrosce.jsmacros.core.config.Option;
import com.jsmacrosce.jsmacros.core.config.OptionType;

import java.util.HashMap;
import java.util.Map;

public class GraalConfig {

    // TODO: Make these language keys dynamic, ex. "Extra {language} Language Options"
    // Extra GraalVM options to pass to the engine
    @Option(translationKey = "jsmacrosce.settings.languages.extraengineoptions", group = {"jsmacrosce.settings.languages", "jsmacrosce.settings.languages.graaloptions"}, type = @OptionType("string"))
    public Map<String, String> extraEngineOptions = new HashMap<>();

    // Extra JS options to pass to the JS engine
    @Option(translationKey = "jsmacrosce.settings.languages.extralangoptions", group = {"jsmacrosce.settings.languages", "jsmacrosce.settings.languages.graaloptions"}, type = @OptionType("string"))
    public Map<String, Map<String, String>> extraLangOptions = new HashMap<>();

    @Deprecated
    public void fromV1(JsonObject v1) {
        // v1 used extraJsOptions specifically for js engine options
        JsonObject obj = v1.getAsJsonObject("extraJsOptions");
        if (obj == null) {
            return;
        }

        Map<String, String> jsOptions = extraLangOptions.computeIfAbsent("js", k -> new HashMap<>());
        for (Map.Entry<String, JsonElement> el : obj.entrySet()) {
            jsOptions.put(el.getKey(), el.getValue().getAsString());
        }
        v1.remove("extraJsOptions");
    }

    @Deprecated
    public void fromV2(JsonObject v2) {
        // v2 used a shared engine, so extraGraalOptions under "js" was specifically for js engine options
        JsonObject v1 = v2.getAsJsonObject("js");
        if (v1 != null && v1.isJsonObject()) {
            JsonObject options = v1.getAsJsonObject("extraGraalOptions");
            Map<String, String> jsOptions = extraLangOptions.computeIfAbsent("js", k -> new HashMap<>());
            if (options != null) {
                for (Map.Entry<String, JsonElement> el : options.entrySet()) {
                    jsOptions.put(el.getKey(), el.getValue().getAsString());
                }

                // This is removed below but kept for consistency anyway
                v1.remove("extraGraalOptions");
            }
        }
        v2.remove("js");
    }

    @Deprecated
    public void fromV3(JsonObject v3) {
        // v3->v4 removes extraGraalOptions and replaces it with a map of language -> options map.
        // Base engine options are added with extraEngineOptions the root engine extra options
        // instead of the js engine specific options and makes a new extraJsOptions for js specific options
        JsonObject graalConfig = v3.getAsJsonObject("graal");
        if (graalConfig != null && graalConfig.isJsonObject()) {
            // These really acted closer to js specific options than graal engine options
            JsonObject options = graalConfig.getAsJsonObject("extraGraalOptions");
            Map<String, String> jsOptions = extraLangOptions.computeIfAbsent("js", k -> new HashMap<>());
            if (options != null) {
                for (Map.Entry<String, JsonElement> el : options.entrySet()) {
                    jsOptions.put(el.getKey(), el.getValue().getAsString());
                }

                graalConfig.remove("extraGraalOptions");
            }
        }
    }
}
