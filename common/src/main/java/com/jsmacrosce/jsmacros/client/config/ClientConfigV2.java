package com.jsmacrosce.jsmacros.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.access.IFontManager;
import com.jsmacrosce.jsmacros.client.gui.screens.EditorScreen;
import com.jsmacrosce.jsmacros.core.config.Option;
import com.jsmacrosce.jsmacros.core.config.OptionType;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClientConfigV2 {
    @Option(translationKey = "jsmacrosce.sort", group = "jsmacrosce.settings.gui")
    public Sorting.MacroSortMethod sortMethod = Sorting.MacroSortMethod.Enabled;

    @Option(translationKey = "jsmacrosce.sortservices", group = "jsmacrosce.settings.gui")
    public Sorting.ServiceSortMethod sortServicesMethod = Sorting.ServiceSortMethod.Enabled;

    @Option(translationKey = "jsmacrosce.showslotindexes", group = "jsmacrosce.settings.gui")
    public boolean showSlotIndexes = false;

    @Option(translationKey = "jsmacrosce.disablewithscreen", group = "jsmacrosce.settings.general")
    public boolean disableKeyWhenScreenOpen = true;

    @Option(translationKey = "jsmacrosce.theme", group = {"jsmacrosce.settings.editor", "jsmacrosce.settings.editor.color"}, getter = "getThemeData", type = @OptionType("color"))
    public Map<String, short[]> editorTheme = null;

    @Option(translationKey = "jsmacrosce.linteroverrides", group = {"jsmacrosce.settings.editor", "jsmacrosce.settings.editor.linter"}, options = "languages", type = @OptionType("file"))
    public Map<String, String> editorLinterOverrides = new HashMap<>();

    @Option(translationKey = "jsmacrosce.history", group = "jsmacrosce.settings.editor")
    public int editorHistorySize = 20;

    @Option(translationKey = "jsmacrosce.autocomplete", group = "jsmacrosce.settings.editor")
    public boolean editorSuggestions = true;

    @Option(translationKey = "jsmacrosce.font", group = "jsmacrosce.settings.editor", options = "getFonts")
    public String editorFont = "jsmacrosce:monocraft";

    @Option(translationKey = "jsmacrosce.useexternaleditor", group = "jsmacrosce.settings.editor")
    public boolean externalEditor = false;

    @Option(translationKey = "jsmacrosce.externaleditorcommand", group = "jsmacrosce.settings.editor")
    public String externalEditorCommand = "code %MacroFolder %File";

    @Option(translationKey = "jsmacrosce.showrunningservices", group = "jsmacrosce.settings.services")
    public boolean showRunningServices = false;

    @Option(translationKey = "jsmacrosce.serviceautoreload", group = "jsmacrosce.settings.services", setter = "setServiceAutoReload")
    public boolean serviceAutoReload = false;

    public List<String> languages() {
        return EditorScreen.langs;
    }

    @SuppressWarnings("resource")
    public List<String> getFonts() {
        return ((IFontManager) Minecraft.getInstance().fontManager).jsmacros_getFontList().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    public Map<String, short[]> getThemeData() {
        if (editorTheme == null) {
            editorTheme = new HashMap<>();
            // JS
            editorTheme.put("keyword", new short[]{0xCC, 0x78, 0x32});
            editorTheme.put("number", new short[]{0x79, 0xAB, 0xFF});
            editorTheme.put("function-variable", new short[]{0x79, 0xAB, 0xFF});
            editorTheme.put("function", new short[]{0xA2, 0xEA, 0x22});
            editorTheme.put("operator", new short[]{0xD8, 0xD8, 0xD8});
            editorTheme.put("string", new short[]{0x12, 0xD4, 0x89});
            editorTheme.put("comment", new short[]{0xA0, 0xA0, 0xA0});
            editorTheme.put("constant", new short[]{0x21, 0xB4, 0x3E});
            editorTheme.put("class-name", new short[]{0x21, 0xB4, 0x3E});
            editorTheme.put("boolean", new short[]{0xFF, 0xE2, 0x00});
            editorTheme.put("punctuation", new short[]{0xD8, 0xD8, 0xD8});
            editorTheme.put("interpolation-punctuation", new short[]{0xCC, 0x78, 0x32});

            //py
            editorTheme.put("builtin", new short[]{0x21, 0xB4, 0x3E});
            editorTheme.put("format-spec", new short[]{0xCC, 0x78, 0x32});

            //regex
            editorTheme.put("regex", new short[]{0x12, 0xD4, 0x89});
            editorTheme.put("charset-negation", new short[]{0xCC, 0x78, 0x32});
            editorTheme.put("charset-punctuation", new short[]{0xD8, 0xD8, 0xD8});
            editorTheme.put("escape", new short[]{0xFF, 0xE2, 0x00});
            editorTheme.put("charclass", new short[]{0xFF, 0xE2, 0x00});
            editorTheme.put("quantifier", new short[]{0x79, 0xAB, 0xFF});
            JsMacrosClient.clientCore.config.saveConfig();
        }
        return editorTheme;
    }

    public void setServiceAutoReload(boolean value) {
        serviceAutoReload = value;
        if (value) {
            JsMacrosClient.clientCore.services.startReloadListener();
        } else {
            JsMacrosClient.clientCore.services.stopReloadListener();
        }
    }

    public Comparator<ScriptTrigger> getSortComparator() {
        if (this.sortMethod == null) {
            this.sortMethod = Sorting.MacroSortMethod.Enabled;
        }
        switch (this.sortMethod) {
            default:
            case Enabled:
                return new Sorting.SortByEnabled();
            case FileName:
                return new Sorting.SortByFileName();
            case TriggerName:
                return new Sorting.SortByTriggerName();
        }
    }

    public Comparator<String> getServiceSortComparator() {
        if (this.sortServicesMethod == null) {
            this.sortServicesMethod = Sorting.ServiceSortMethod.Enabled;
        }
        switch (this.sortServicesMethod) {
            case Enabled:
                return new Sorting.SortServiceByEnabled(JsMacrosClient.clientCore);
            case Name:
                return new Sorting.SortServiceByName();
            case Running:
                return new Sorting.SortServiceByRunning(JsMacrosClient.clientCore);
            case FileName:
                return new Sorting.SortServiceByFileName(JsMacrosClient.clientCore);
            default:
                throw new IllegalArgumentException();
        }
    }

    @Deprecated
    public void fromV1(JsonObject v1) {
        sortMethod = Sorting.MacroSortMethod.valueOf(v1.get("sortMethod").getAsString());
        v1.remove("sortMethod");
        disableKeyWhenScreenOpen = v1.get("disableKeyWhenScreenOpen").getAsBoolean();
        v1.remove("disableKeyWhenScreenOpen");
        if (v1.has("editorTheme") && v1.get("editorTheme").isJsonObject()) {
            editorTheme = new HashMap<>();
            for (Map.Entry<String, JsonElement> el : v1.getAsJsonObject("editorTheme").entrySet()) {
                short[] color = new short[3];
                int i = 0;
                for (JsonElement el2 : el.getValue().getAsJsonArray()) {
                    color[i] = el2.getAsShort();
                    ++i;
                }
                editorTheme.put(el.getKey(), color);
            }
        }
        v1.remove("editorTheme");
        editorLinterOverrides = new HashMap<>();
        if (v1.has("editorLinterOverrides") && v1.get("editorLinterOverrides").isJsonObject()) {
            for (Map.Entry<String, JsonElement> el : v1.getAsJsonObject("editorLinterOverrides").entrySet()) {
                editorLinterOverrides.put(el.getKey(), el.getValue().getAsString());
            }
        }
        v1.remove("editorLinterOverrides");
        editorHistorySize = v1.get("editorHistorySize").getAsInt();
        v1.remove("editorHistorySize");
        editorSuggestions = v1.get("editorSuggestions").getAsBoolean();
        v1.remove("editorSuggestions");
        editorFont = v1.get("editorFont").getAsString();
        v1.remove("editorFont");
    }

}
