package com.jsmacrosce.jsmacros.client.config;

import com.jsmacrosce.doclet.DocletIgnore;
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

/** Client-side GUI, input, and editor options registered with the client core configuration.
 * Scripts can retrieve this options object from the global config manager:
 * <pre>
 * const ClientConfig = Java.type("com.jsmacrosce.jsmacros.client.config.ClientConfigV2");
 * const options = JsMacros.getConfig().getOptions(ClientConfig);
 * if (options !== null) {
 *   Chat.log(options.editorHistorySize);
 * }
 * </pre>
 * The options object is not the global configuration itself; assigning fields does not by itself save the config or rebuild an open editor.
 */
public class ClientConfigV2 {
    /** Controls the ordering of macros in the GUI macro list; defaults to {@code Enabled}.
     * {@code getSortComparator()} resets a null value to {@code Enabled} when called. */
    @Option(translationKey = "jsmacrosce.sort", group = "jsmacrosce.settings.gui")
    public Sorting.MacroSortMethod sortMethod = Sorting.MacroSortMethod.Enabled;

    /** Controls the ordering of services in the GUI service list; defaults to {@code Enabled}.
     * {@code getServiceSortComparator()} resets a null value to {@code Enabled} when called. */
    @Option(translationKey = "jsmacrosce.sortservices", group = "jsmacrosce.settings.gui")
    public Sorting.ServiceSortMethod sortServicesMethod = Sorting.ServiceSortMethod.Enabled;

    /** Enables zero-based menu slot-list indexes in the active slots rendered by the handled-screen hook; defaults to {@code false}. */
    @Option(translationKey = "jsmacrosce.showslotindexes", group = "jsmacrosce.settings.gui")
    public boolean showSlotIndexes = false;

    /** Suppresses most JsMacros key-event dispatch while a screen is open; defaults to {@code true}.
     * Releases matching presses begun without a screen remain eligible. Disabling this option does not bypass
     * the other screen filters, and this check does not prevent the preceding key-state ledger update. */
    @Option(translationKey = "jsmacrosce.disablewithscreen", group = "jsmacrosce.settings.general")
    public boolean disableKeyWhenScreenOpen = true;

    /** Editor syntax-token colors, mapped from token names to red, green, and blue components using their low 8 bits.
     * Color arrays need at least three entries. Initially {@code null}; {@code getThemeData()} creates defaults
     * and saves the config when the field is null. A supplied non-null map is reused without merging defaults. */
    @Option(translationKey = "jsmacrosce.theme", group = {"jsmacrosce.settings.editor", "jsmacrosce.settings.editor.color"}, getter = "getThemeData", type = @OptionType("color"))
    public Map<String, short[]> editorTheme = null;

    /** Maps editor language identifiers to linter script paths resolved relative to the macro folder; an empty map uses built-in compilers. The language {@code none} bypasses overrides. */
    @Option(translationKey = "jsmacrosce.linteroverrides", group = {"jsmacrosce.settings.editor", "jsmacrosce.settings.editor.linter"}, options = "languages", type = @OptionType("file"))
    public Map<String, String> editorLinterOverrides = new HashMap<>();

    /** Maximum undo-history entries for each newly created editor history; defaults to 20.
     * Use a positive count: zero or negative values can fail during construction or edits.
     * Existing histories keep the limit read at their construction; grouped edits may share one entry. */
    @Option(translationKey = "jsmacrosce.history", group = "jsmacrosce.settings.editor")
    public int editorHistorySize = 20;

    /** Enables suggestion production by the built-in compiler and display of suggestions in the editor; defaults to {@code true}. */
    @Option(translationKey = "jsmacrosce.autocomplete", group = "jsmacrosce.settings.editor")
    public boolean editorSuggestions = true;

    /** Resource identifier string used to select the editor font; defaults to {@code jsmacrosce:monocraft}. */
    @Option(translationKey = "jsmacrosce.font", group = "jsmacrosce.settings.editor", options = "getFonts")
    public String editorFont = "jsmacrosce:monocraft";

    /** When enabled, tries the configured external command for an existing regular file; defaults to {@code false}.
     * A successful process start skips the internal editor; caught process-start I/O failures allow fallback.
     * This does not wait for the external process or check its exit status. */
    @Option(translationKey = "jsmacrosce.useexternaleditor", group = "jsmacrosce.settings.editor")
    public boolean externalEditor = false;

    /** Command template split on whitespace before substitution, without shell-style quote parsing.
     * Supports {@code %File} (absolute file path), {@code %MacroFolder} (absolute macro folder),
     * and {@code %Folder} (absolute file parent). Substituted paths remain in their existing argument tokens.
     * Defaults to {@code code %MacroFolder %File}. */
    @Option(translationKey = "jsmacrosce.externaleditorcommand", group = "jsmacrosce.settings.editor")
    public String externalEditorCommand = "code %MacroFolder %File";

    /** Controls whether service-triggered contexts are included in the running-scripts screen; defaults to {@code false}.
     * A new screen initializes its service checkbox from this value; changing it does not update an open screen. */
    @Option(translationKey = "jsmacrosce.showrunningservices", group = "jsmacrosce.settings.services")
    public boolean showRunningServices = false;

    /** Stores whether service file-change reload processing is enabled; defaults to {@code false}.
     * Client startup uses this value to start the reload listener. Use {@code setServiceAutoReload(boolean)} to also update the current manager. */
    @Option(translationKey = "jsmacrosce.serviceautoreload", group = "jsmacrosce.settings.services", setter = "setServiceAutoReload")
    public boolean serviceAutoReload = false;

    /** Returns the editor's shared language-identifier list.
     * @return the mutable list used by the editor */
    public List<String> languages() {
        return EditorScreen.langs;
    }

    /** Collects font resource identifiers from the client font manager.
     * @return a newly collected list of font resource identifier strings; order is unspecified */
    @SuppressWarnings("resource")
    public List<String> getFonts() {
        return ((IFontManager) Minecraft.getInstance().fontManager).jsmacros_getFontList().stream().map(ResourceLocation::toString).collect(Collectors.toList());
    }

    /** Returns the editor token-color map, initializing defaults and saving the config if it is {@code null}.
     * The returned map is reused directly; non-null maps are not cloned, validated, or merged with defaults.
     * @return the live mutable token-name-to-RGB map; each color array must provide at least three components, whose low 8 bits are used */
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

    /** Stores the service auto-reload preference and starts or stops reload processing in the current client manager.
     * @param value whether to enable file-change reload processing */
    public void setServiceAutoReload(boolean value) {
        serviceAutoReload = value;
        if (value) {
            JsMacrosClient.clientCore.services.startReloadListener();
        } else {
            JsMacrosClient.clientCore.services.stopReloadListener();
        }
    }

    /** Returns a new comparator selected by {@code sortMethod}; a {@code null} setting is reset to {@code Enabled}.
     * The comparator compares triggers but does not sort a collection.
     * @return a new macro-trigger comparator for the selected ordering */
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

    /** Returns a new service-name comparator selected by {@code sortServicesMethod}; a {@code null} setting is reset to {@code Enabled}.
     * Enabled and running comparisons query the current client service manager when comparing; selection does not reorder services.
     * @return a new service-name comparator for the selected ordering */
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

    @DocletIgnore
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
