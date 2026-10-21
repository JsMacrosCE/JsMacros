package com.jsmacrosce.jsmacros.core.config;

import com.jsmacrosce.doclet.DocletIgnore;
import com.google.gson.annotations.SerializedName;

import java.nio.file.Path;

/**
 * Mutable trigger configuration used by listeners; constructing one does not register a listener or run a script.
 * <pre>
 * const Trigger = Java.type("com.jsmacrosce.jsmacros.core.config.ScriptTrigger");
 * const Type = Java.type("com.jsmacrosce.jsmacros.core.config.ScriptTrigger$TriggerType");
 * const Path = Java.type("java.nio.file.Path");
 * const trigger = new Trigger(Type.EVENT, "Tick", Path.of("script.js"), false, false);
 * Chat.log(trigger.getEvent());
 * </pre>
 */
public class ScriptTrigger {
    /** Selects event-name dispatch or a key-event mode. */
    @SerializedName(value = "triggerType", alternate = "type")
    public TriggerType triggerType;
    /** Event name, or key and modifier combination for key triggers. */
    @SerializedName(value = "event", alternate = "eventkey")
    public String event;
    /** Configured script path. */
    public Path scriptFile;
    /** Whether listener execution is allowed; changing this does not stop an in-flight run. */
    public boolean enabled;
    /** Whether joined handling is requested where the event supports it. */
    public boolean joined;

    /** Initializes the trigger configuration; the path is stringified and reconstructed by the other constructor.
     * @param triggerType dispatch mode
     * @param event event name or key/modifier combination
     * @param scriptFile configured script path
     * @param enabled whether listener execution is allowed
     * @param joined whether to request joined handling where supported
     */
    public ScriptTrigger(TriggerType triggerType, String event, Path scriptFile, boolean enabled, boolean joined) {
        this(triggerType, event, scriptFile.toString(), enabled, joined);
    }

    @DocletIgnore
    @Deprecated
    public ScriptTrigger(TriggerType triggerType, String event, String scriptFile, boolean enabled, boolean joined) {
        this.triggerType = triggerType;
        this.event = event;
        this.scriptFile = Path.of(scriptFile);
        this.enabled = enabled;
        this.joined = joined;
    }

    /** Compares trigger type, case-insensitive event text, and path; enabled and joined are ignored.
     * @param macro trigger to compare
     * @return whether those three configuration values match
     */
    public boolean equals(ScriptTrigger macro) {
        return triggerType == macro.triggerType && event.equalsIgnoreCase(macro.event) && scriptFile.equals(macro.scriptFile);
    }

    /** Returns a diagnostic description of the configured type, event, path, enabled flag, and joined flag; this is not a serialization format.
     * @return diagnostic description
     */
    public String toString() {
        return String.format("RawMacro:{\"type\": \"%s\", \"eventkey\": \"%s\", \"scriptFile\": \"%s\", \"enabled\": %b, \"joined\": %b}", triggerType.toString(), event, scriptFile, enabled, joined);
    }

    /** Copies a trigger's configuration into a new instance.
     * @param m trigger to copy
     * @return new trigger with the same configuration
     */
    public static ScriptTrigger copy(ScriptTrigger m) {
        return new ScriptTrigger(m.triggerType, m.event, m.scriptFile, m.enabled, m.joined);
    }

    /** Returns a new trigger with this instance's configuration.
     * @return new trigger with the same configuration
     */
    public ScriptTrigger copy() {
        return copy(this);
    }

    /**
     * Selects a key-event mode or dispatch by a named event. This value configures a trigger; it does not register a listener or run a script.
     * <pre>
     * const Trigger = Java.type("com.jsmacrosce.jsmacros.core.config.ScriptTrigger");
     * const Type = Java.type("com.jsmacrosce.jsmacros.core.config.ScriptTrigger$TriggerType");
     * const Path = Java.type("java.nio.file.Path");
     * const trigger = new Trigger(Type.EVENT, "Tick", Path.of("script.js"), false, false);
     * trigger.getTriggerType();
     * </pre>
     * @author Wagyourtail
     * @since 1.2.9
     */
    public enum TriggerType {
        /** Matches configured key and modifier events whose action is not {@code 1}. */
        KEY_FALLING,
        /** Runs for matching key and modifier events with action {@code 1}. */
        KEY_RISING,
        /** Runs for matching key and modifier events without filtering by action. */
        KEY_BOTH,
        /** Registers a listener for the configured event name rather than the key event. */
        EVENT
    }

    /**
     * Returns this trigger's type.
     * @return the configured trigger type
     * @since 1.2.7
     */
    public TriggerType getTriggerType() {
        return triggerType;
    }

    /**
     * Returns the event or key identifier associated with this trigger.
     * @return the configured event/key name
     * @since 1.2.7
     */
    public String getEvent() {
        return event;
    }

    /**
     * Returns the script file path as a string.
     * @return the path's string representation
     * @since 1.2.7
     */
    public String getScriptFile() {
        return scriptFile.toString();
    }

    /**
     * Returns the configured script path.
     * @since 2.0.0
     * @return the script path
     */
    public Path getScriptPath() {
        return scriptFile;
    }

    /**
     * Returns whether this trigger is enabled.
     * @return {@code true} when enabled
     * @since 1.2.7
     */
    public boolean getEnabled() {
        return enabled;
    }

}
