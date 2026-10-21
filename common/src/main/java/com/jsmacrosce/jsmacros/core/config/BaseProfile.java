package com.jsmacrosce.jsmacros.core.config;

import org.slf4j.Logger;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.EventLockWatchdog;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.BaseEventRegistry;
import com.jsmacrosce.jsmacros.core.event.IEventListener;
import com.jsmacrosce.jsmacros.core.event.impl.EventCustom;
import com.jsmacrosce.jsmacros.core.event.impl.EventProfileLoad;
import com.jsmacrosce.jsmacros.core.event.impl.EventWrappedScript;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;
import com.jsmacrosce.jsmacros.core.language.EventContainer;
import com.jsmacrosce.jsmacros.core.library.impl.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages a core's active profile and the event and library registries initialized for it.
 * @author Wagyourtail
 * @since 1.2.7
 */
@DocletCategory("Configuration/Profiles")
public abstract class BaseProfile {
    /** Core associated with this profile. */
    public final Core<?, ?> runner;
    /** Logger supplied for this profile's diagnostics. */
    public final Logger LOGGER;
    /**
     * Threads joined during joined-main event dispatch, removed after their callbacks are awaited or interrupted.
     * This mutable set is neither a thread stack nor a record of all running threads.
     */
    public final Set<Thread> joinedThreadStack = new HashSet<>();

    /** Current profile name, or {@code null} until one is assigned; this name is used as the save key. */
    public String profileName;

    /**
     * Stores the supplied core and logger references; registry setup and profile loading are performed by {@code init(String)}.
     * @param runner core associated with this profile
     * @param logger logger for profile diagnostics
     */
    public BaseProfile(Core runner, Logger logger) {
        this.runner = runner;
        this.LOGGER = logger;
    }

    /** Reports a failure through this profile's error-handling implementation.
     * @param ex failure to report
     */
    public abstract void logError(Throwable ex);

    /**
     * Returns this profile's core event registry.
     * @return the registry
     * @since 1.2.2
     */
    @DocletIgnore
    @Deprecated
    public BaseEventRegistry getRegistry() {
        return runner.eventRegistry;
    }

    /**
     * Reports whether the current thread's joined-event stack permits joined dispatch.
     * @return {@code true} if joined dispatch is permitted for the current thread
     * @since 1.6.0
     */
    public abstract boolean checkJoinedThreadStack();

    /**
     * Loads the named profile, creating and persisting an empty profile when its name is absent from configuration.
     * @param profileName name of the profile to load or create
     * @since 1.0.9
     */
    public void loadOrCreateProfile(String profileName) {
        runner.eventRegistry.clearMacros();
        if (runner.config.getOptions(CoreConfigV2.class).profiles.containsKey(profileName)) {
            loadProfile(profileName);
        } else {
            runner.config.getOptions(CoreConfigV2.class).profiles.put(profileName, new ArrayList<>());
            loadProfile(profileName);
            runner.config.saveConfig();
        }
    }

    /**
     * Replaces registered script triggers with those stored for the named profile and fires its profile-load event.
     * Triggers are cleared even when no data exists.
     * @param pName name of the profile to load
     * @return {@code true} if profile data exists and was loaded; {@code false} otherwise
     * @since 1.5.0
     */
    protected boolean loadProfile(String pName) {
        runner.eventRegistry.clearMacros();
        final List<ScriptTrigger> rawProfile = runner.config.getOptions(CoreConfigV2.class).profiles.get(pName);
        if (rawProfile == null) {
            LOGGER.warn("profile \"" + pName + "\" does not exist or is broken/null");
            return false;
        }
        renameCurrentProfile(pName);
        for (ScriptTrigger rawmacro : rawProfile) {
            runner.eventRegistry.addScriptTrigger(rawmacro);
        }
        new EventProfileLoad(this, pName).trigger();

        return true;
    }

    /**
     * Saves the registered script triggers under the current profile name and persists the configuration.
     * @since 1.0.0
     */
    public void saveProfile() {
        runner.config.getOptions(CoreConfigV2.class).profiles.put(getCurrentProfileName(), runner.eventRegistry.getScriptTriggers());
        runner.config.saveConfig();
    }

    /**
     * Dispatches an event to its named listeners and, unless ignored, to {@code ANYTHING} listeners.
     * Joined dispatch is used for joined listeners or cancellable events when the event is joinable
     * (or its name is configured as joinable); joined callbacks are awaited.
     * @param event event to dispatch
     * @since 1.2.7
     */
    public void triggerEvent(BaseEvent event) {
        boolean joinedMain = checkJoinedThreadStack();
        if (event instanceof EventCustom) {
            for (IEventListener macro : runner.eventRegistry.getListeners(((EventCustom) event).eventName)) {
                if ((macro.joined() || event.cancellable()) && event.joinable()) {
                    runJoinedEventListener(event, joinedMain, macro);
                } else {
                    macro.trigger(event);
                }
            }

            if (!runner.config.getOptions(CoreConfigV2.class).anythingIgnored.contains(((EventCustom) event).eventName)) {
                for (IEventListener macro : runner.eventRegistry.getListeners("ANYTHING")) {
                    if ((macro.joined() || event.cancellable()) && event.joinable()) {
                        runJoinedEventListener(event, joinedMain, macro);
                    } else {
                        macro.trigger(event);
                    }
                }
            }
        } else {
            String eventName = event.getEventName();
            for (IEventListener macro : runner.eventRegistry.getListeners(eventName)) {
                if ((macro.joined() || event.cancellable()) && (event.joinable() || runner.eventRegistry.joinableEvents.contains(eventName))) {
                    runJoinedEventListener(event, joinedMain, macro);
                } else {
                    macro.trigger(event);
                }
            }

            if (!runner.config.getOptions(CoreConfigV2.class).anythingIgnored.contains(eventName)) {
                for (IEventListener macro : runner.eventRegistry.getListeners("ANYTHING")) {
                    if ((macro.joined() || event.cancellable()) && (event.joinable() || runner.eventRegistry.joinableEvents.contains(eventName))) {
                        runJoinedEventListener(event, joinedMain, macro);
                    } else {
                        macro.trigger(event);
                    }
                }
            }
        }
    }

    protected void runJoinedEventListener(BaseEvent event, boolean joinedMain, IEventListener macroListener) {
        if (macroListener instanceof FJsMacros.ScriptEventListener) {
            BaseScriptContext<?> ctx = ((FJsMacros.ScriptEventListener) macroListener).getCtx();
            if (ctx != null && ctx.getBoundThreads().contains(Thread.currentThread()) && !ctx.isMultiThreaded()) {
                if (!macroListener.joined()) {
                    ((FJsMacros.ScriptEventListener) macroListener).triggerInline(event);
                    return;
                }
                throw new IllegalThreadStateException("Cannot join " + macroListener + " on same context as it's creation.");
            }
        }
        EventContainer<?> t = macroListener.trigger(event);
        if (t == null) {
            return;
        }
        try {
            if (joinedMain) {
                joinedThreadStack.add(t.getLockThread());
            }
            EventLockWatchdog.startWatchdog(t, macroListener, runner.config.getOptions(CoreConfigV2.class).maxLockTime);
            t.awaitLock(() -> joinedThreadStack.remove(t.getLockThread()));
        } catch (InterruptedException ignored) {
            joinedThreadStack.remove(t.getLockThread());
        }
    }

    /** Initializes registries, then loads or creates the default profile.
     * @param defaultProfile name of the profile to load or create
     */
    public void init(String defaultProfile) {
        initRegistries();
        loadOrCreateProfile(defaultProfile);
    }

    /** Returns the current stored profile name.
     * @return current name, or {@code null} before one is assigned
     */
    public String getCurrentProfileName() {
        return profileName;
    }

    /** Assigns the current profile name without renaming a configured profile entry or persisting configuration.
     * @param profile new current profile name
     */
    public void renameCurrentProfile(String profile) {
        profileName = profile;
    }

    /**
     * Registers the core's default events and libraries during profile initialization. Override to register
     * additional entries; this initialization hook is not intended for script invocation.
     */
    protected void initRegistries() {
        runner.eventRegistry.addEvent("ANYTHING", true, true);
        runner.eventRegistry.addEvent(EventProfileLoad.class);
        runner.eventRegistry.addEvent(EventWrappedScript.class);

        runner.libraryRegistry.addLibrary(FJsMacros.class);
        runner.libraryRegistry.addLibrary(FFS.class);
        runner.libraryRegistry.addLibrary(FGlobalVars.class);
        runner.libraryRegistry.addLibrary(FReflection.class);
        runner.libraryRegistry.addLibrary(FRequest.class);
        runner.libraryRegistry.addLibrary(FTime.class);
    }

}
