package com.jsmacrosce.jsmacros.core.config;

import com.jsmacrosce.doclet.DocletIgnore;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.event.Event;
import com.jsmacrosce.jsmacros.core.event.impl.EventProfileLoad;
import com.jsmacrosce.jsmacros.core.service.ServiceTrigger;

import java.util.*;

/**
 * Holds core profile, service, and event-dispatch settings. Obtain the managed instance through the
 * configuration manager; its runner is injected by that manager.
 * <pre>
 * const config = JsMacros.getConfig().getOptions(Java.type("com.jsmacrosce.jsmacros.core.config.CoreConfigV2"));
 * Chat.log(`${config.getCurrentProfile()}: ${config.profileOptions()}`);
 * </pre>
 */
public class CoreConfigV2 {
    
    private transient Core<?, ?> runner;

    /** Configured nonnegative delay, in milliseconds, before the joined-lock watchdog's timed check; initially {@code 500}. This is not an exact deadline or scheduling guarantee. */
    @Option(translationKey = "jsmacrosce.maxlocktime", group = "jsmacrosce.settings.general")
    public long maxLockTime = 500;

    /** Profile name used when the core initializes its profile manager; initially {@code "default"}. */
    @Option(translationKey = "jsmacrosce.defaultprofile", group = "jsmacrosce.settings.profile", options = "profileOptions")
    public String defaultProfile = "default";

    /** Event names omitted only from dispatch to {@code ANYTHING} listeners; named listeners still receive them. */
    @Option(translationKey = "jsmacrosce.anythingIgnored", group = { "jsmacrosce.settings.general", "jsmacrosce.anythingIgnored" }, options = "getEvents")
    public List<String> anythingIgnored = new ArrayList<>(Arrays.asList(
            "Sound",
            "Tick",
            "RecvPacket",
            "SendPacket"
    ));

    /** Maps profile names to their mutable script-trigger lists; construction seeds the {@code default} entry. */
    @Option(translationKey = "jsmacrosce.profiles", group = {"jsmacrosce.settings.profile", "jsmacrosce.settings.profile.list"}, type = @OptionType("profile"))
    public Map<String, List<ScriptTrigger>> profiles = new HashMap<>();

    /** Configured service triggers, keyed by service name; this is not the running-service registry. */
    public Map<String, ServiceTrigger> services = new HashMap<>();

    /** Seeds the default profile with its enabled, non-joined {@code ProfileLoad} {@code index.js} trigger. */
    public CoreConfigV2() {
        profiles.put("default", new ArrayList<>());
        profiles.get("default").add(new ScriptTrigger(ScriptTrigger.TriggerType.EVENT, EventProfileLoad.class.getAnnotation(Event.class).value(), "index.js", true, false));
    }

    //"synthetic" option
    /** Returns the active profile name. Requires the runner injected into the manager-owned configuration instance.
     * @return the current profile name
     */
    @Option(translationKey = "jsmacrosce.currentprofile", group = "jsmacrosce.settings.profile", setter = "setCurrentProfile", options = "profileOptions")
    public String getCurrentProfile() {
        return runner.profile.getCurrentProfileName();
    }

    /** Saves the active profile, then loads or creates the named profile; requires a manager-owned instance with its runner injected.
     * @param pname name of the profile to load or create
     */
    public void setCurrentProfile(String pname) {
        runner.profile.saveProfile();
        runner.profile.loadOrCreateProfile(pname);
    }

    /** Returns a new list containing the configured profile names.
     * @return a fresh list of profile names
     */
    public List<String> profileOptions() {
        return new ArrayList<>(profiles.keySet());
    }

    /** Returns a snapshot of registered event names; requires the runner injected into the manager-owned instance.
     * @return a fresh list of event names
     */
    public List<String> getEvents() {
        return new ArrayList<>(runner.eventRegistry.events);
    }

    @DocletIgnore
    @Deprecated
    public void fromV1(JsonObject v1) {
        defaultProfile = v1.get("defaultProfile").getAsString();
        v1.remove("defaultProfile");
        profiles = new HashMap<>();
        for (Map.Entry<String, JsonElement> el : v1.getAsJsonObject("profiles").entrySet()) {
            List<ScriptTrigger> triggers = new LinkedList<>();
            for (JsonElement el2 : el.getValue().getAsJsonArray()) {
                triggers.add(ConfigManager.gson.fromJson(el2, ScriptTrigger.class));
            }
            profiles.put(el.getKey(), triggers);
        }
        v1.remove("profiles");
    }

}
