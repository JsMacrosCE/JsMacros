package com.jsmacrosce.jsmacros.core.library.impl;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.doclet.DocletReplaceReturn;
import com.jsmacrosce.doclet.DocletReplaceTypeParams;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.config.BaseProfile;
import com.jsmacrosce.jsmacros.core.config.ConfigManager;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.event.*;
import com.jsmacrosce.jsmacros.core.event.impl.EventCustom;
import com.jsmacrosce.jsmacros.core.event.impl.FiltererComposed;
import com.jsmacrosce.jsmacros.core.event.impl.FiltererInverted;
import com.jsmacrosce.jsmacros.core.event.impl.FiltererModulus;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;
import com.jsmacrosce.jsmacros.core.language.EventContainer;
import com.jsmacrosce.jsmacros.core.library.Library;
import com.jsmacrosce.jsmacros.core.library.PerExecLibrary;
import com.jsmacrosce.jsmacros.core.library.impl.classes.WrappedScript;
import com.jsmacrosce.jsmacros.core.service.ServiceManager;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * Functions that interact directly with JsMacros or Events.
 * <p>
 * An instance of this class is passed to scripts as the {@code JsMacros} variable.
 * <br>
 * This is the library the rest of JsMacros is reached through. The three groups of function here
 * are subscribing to events with {@link #on(String, MethodWrapper) on},
 * {@link #once(String, MethodWrapper) once} and
 * {@link #disableScriptListeners(String) disableScriptListeners}, running other scripts with
 * {@link #runScript(String) runScript} and {@link #wrapScriptRun(String) wrapScriptRun}, and
 * blocking on a single event with {@link #waitForEvent(String) waitForEvent}. There is also a
 * handle on the profile itself through {@link #getProfile()}, {@link #getConfig()} and
 * {@link #getServiceManager()}, which is the only way a script reaches the service and macro
 * configuration the GUI edits.
 * <br>
 * A listener is a callback the event system calls, so it is a plain script function and nothing
 * runs until the event actually arrives. A script that subscribes and then returns is not a
 * problem in itself, and no extra keep-alive step is needed: registering a listener means wrapping
 * the function with {@code JavaWrapper.methodToJava}, and making that wrapper is what marks the
 * run as still wanted, so the context is left open rather than closed when the script returns.
 * What does take a listener off again is {@link #off(IEventListener) off} or one of the
 * {@code disableScriptListeners} calls.
 * example:
 * <pre>
 * // a listener: nothing happens until the event fires, which for a tick is twenty
 * // times a second
 * const listener = JsMacros.on("Tick", JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`tick, and the event is called ${event.getEventName()}`);
 * }));
 *
 * // the listener can be taken off again by the handle it returned
 * JsMacros.off(listener);
 *
 * // a one-shot listener, for a thing that only needs catching once
 * JsMacros.once("Disconnect", JavaWrapper.methodToJava(function () {
 *   Chat.log("left the server");
 * }));
 *
 * // a listener with a filterer on an event that fires far too often to handle one
 * // by one: this one only runs once every twenty ticks
 * JsMacros.on("Tick", JsMacros.createModulusEventFilterer(20),
 *   JavaWrapper.methodToJava(function () {
 *     Chat.log("about a second went by");
 *   }));
 *
 * // run a whole other script file and carry on when it finishes. the null is the
 * // event to run it with, and it has to be passed rather than left out: the two
 * // argument form reads its first argument as a language rather than as a path
 * JsMacros.runScript("helper.js", null, JavaWrapper.methodToJava(function () {
 *   Chat.log("helper.js is done");
 * }));
 * </pre>
 *
 * @author Wagyourtail
 */
@Library("JsMacros")
@SuppressWarnings("unused")
public class FJsMacros extends PerExecLibrary {

    public FJsMacros(BaseScriptContext<?> context) {
        super(context);
    }

    /**
     * @return the JsMacros profile class.
     */
    public BaseProfile getProfile() {
        return runner.profile;
    }

    /**
     * @return the JsMacros config management class.
     */
    public ConfigManager getConfig() {
        return runner.config;
    }

    /**
     * services are background scripts designed to run full time and are mainly noticed by their side effects.
     *
     * @return for managing services.
     * @since 1.6.3
     */
    public ServiceManager getServiceManager() {
        return runner.services;
    }

    /**
     * @return list of non-garbage-collected ScriptContext's
     * @since 1.4.0
     */
    public List<BaseScriptContext<?>> getOpenContexts() {
        return ImmutableList.copyOf(runner.getContexts());
    }

    /**
     * @param file
     * @see FJsMacros#runScript(String, String, MethodWrapper)
     * @since 1.1.5
     */
    public EventContainer<?> runScript(String file) {
        return runScript(file, (EventCustom) null, null);
    }

    /**
     * @param file
     * @param fakeEvent you probably actually want to pass an instance created by {@link #createCustomEvent(String)}
     * @return
     * @since 1.6.3
     */
    public EventContainer<?> runScript(String file, @Nullable BaseEvent fakeEvent) {
        return runScript(file, fakeEvent, null);
    }

    /**
     * runs a script with a eventCustom to be able to pass args
     *
     * @param file
     * @param fakeEvent
     * @param callback
     * @return container the script is running on.
     * @since 1.6.3 (1.1.5 - 1.6.3 didn't have fakeEvent)
     */
    public EventContainer<?> runScript(String file, @Nullable BaseEvent fakeEvent, @Nullable MethodWrapper<Throwable, Object, Object, ?> callback) {
        if (callback != null) {
            return runner.exec(new ScriptTrigger(ScriptTrigger.TriggerType.EVENT, "", Path.of(file), true, false), fakeEvent, () -> callback.accept(null), callback);
        } else {
            return runner.exec(new ScriptTrigger(ScriptTrigger.TriggerType.EVENT, "", Path.of(file), true, false), fakeEvent, null, null);
        }
    }

    /**
     * @param language
     * @param script
     * @return
     * @see FJsMacros#runScript(String, String, MethodWrapper)
     * @since 1.2.4
     */
    public EventContainer<?> runScript(String language, String script) {
        return runScript(language, script, null);
    }

    /**
     * Runs a string as a script.
     *
     * @param language
     * @param script
     * @param callback calls your method as a {@link java.util.function.Consumer Consumer}&lt;{@link String}&gt;
     * @return the {@link EventContainer} the script is running on.
     * @since 1.2.4
     */
    public EventContainer<?> runScript(String language, String script, @Nullable MethodWrapper<Throwable, Object, Object, ?> callback) {
        return runScript(language, script, null, callback);
    }

    /**
     * @param language
     * @param script
     * @param file
     * @param callback
     * @return
     * @since 1.6.0
     */
    public EventContainer<?> runScript(String language, String script, @Nullable String file, @Nullable MethodWrapper<Throwable, Object, Object, ?> callback) {
        return runScript(language, script, file, null, callback);
    }

    /**
     * @param language
     * @param script
     * @param file
     * @param event
     * @param callback
     * @return
     * @since 1.7.0
     */
    public EventContainer<?> runScript(String language, String script, @Nullable String file, @Nullable BaseEvent event, @Nullable MethodWrapper<Throwable, Object, Object, ?> callback) {
        if (callback != null) {
            return runner.exec(language, script, file != null ? ctx.getContainedFolder().toPath().resolve(file).toFile() : null, event, () -> callback.accept(null), callback);
        } else {
            return runner.exec(language, script, file != null ? ctx.getContainedFolder().toPath().resolve(file).toFile() : null, event, null, null);
        }
    }

    /**
     * @param file
     * @param <T>
     * @param <U>
     * @param <R>
     * @return
     * @since 1.7.0
     */
    public <T, U, R> MethodWrapper<T, U, R, ?> wrapScriptRun(String file) {
        return new WrappedScript<>(runner, (e) -> (EventContainer<BaseScriptContext<?>>) runner.exec(new ScriptTrigger(ScriptTrigger.TriggerType.EVENT, e.getEventName(), Path.of(file), true, false), e, null, null), false);
    }

    /**
     * @param language
     * @param script
     * @param <T>
     * @param <U>
     * @param <R>
     * @return
     * @since 1.7.0
     */
    public <T, U, R> MethodWrapper<T, U, R, ?> wrapScriptRun(String language, String script) {
        return new WrappedScript<>(runner, (e) -> (EventContainer<BaseScriptContext<?>>) runner.exec(language, script, null, e, null, null), false);
    }

    /**
     * @param language
     * @param script
     * @param file
     * @param <T>
     * @param <U>
     * @param <R>
     * @return
     * @since 1.7.0
     */
    public <T, U, R> MethodWrapper<T, U, R, ?> wrapScriptRun(String language, String script, @Nullable String file) {
        return new WrappedScript<>(runner, (e) -> (EventContainer<BaseScriptContext<?>>) runner.exec(language, script, file != null ? ctx.getContainedFolder().toPath().resolve(file).toFile() : null, e, null, null), false);
    }

    /**
     * @param file
     * @param <T>
     * @param <U>
     * @param <R>
     * @return
     * @since 1.7.0
     */
    public <T, U, R> MethodWrapper<T, U, R, ?> wrapScriptRunAsync(String file) {
        return new WrappedScript<>(runner, (e) -> (EventContainer<BaseScriptContext<?>>) runner.exec(new ScriptTrigger(ScriptTrigger.TriggerType.EVENT, e.getEventName(), Path.of(file), true, false), e, null, null), true);
    }

    /**
     * @param language
     * @param script
     * @param <T>
     * @param <U>
     * @param <R>
     * @return
     * @since 1.7.0
     */
    public <T, U, R> MethodWrapper<T, U, R, ?> wrapScriptRunAsync(String language, String script) {
        return new WrappedScript<>(runner, (e) -> (EventContainer<BaseScriptContext<?>>) runner.exec(language, script, null, e, null, null), true);
    }

    /**
     * @param language
     * @param script
     * @param file
     * @param <T>
     * @param <U>
     * @param <R>
     * @return
     * @since 1.7.0
     */
    public <T, U, R> MethodWrapper<T, U, R, ?> wrapScriptRunAsync(String language, String script, @Nullable String file) {
        return new WrappedScript<>(runner, (e) -> (EventContainer<BaseScriptContext<?>>) runner.exec(language, script, file != null ? ctx.getContainedFolder().toPath().resolve(file).toFile() : null, e, null, null), true);
    }

    // TODO: Migrate open and openUrl to client-only Utils library and deprecate these methods.
    /**
     * Opens a file with the default system program.
     *
     * @param path relative to the script's folder.
     * @since 1.1.8
     */
    @Deprecated
    public void open(String path) throws IOException {
        openUrl(ctx.getContainedFolder().toPath().resolve(path).toUri().toURL());
    }

    /**
     * @param url
     * @throws MalformedURLException
     * @since 1.6.0
     */
    @Deprecated
    public void openUrl(String url) throws IOException {
        openUrl(new URL(url));
    }

    protected void openUrl(URL url) throws IOException {
        String string = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String urlOpen[];
        if (string.contains("mac") || string.contains("darwin")) {
            urlOpen = new String[]{"open", url.toString()};
        } else if (string.contains("win")) {
            urlOpen = new String[]{"rundll32", "url.dll,FileProtocolHandler", url.toString()};
        } else {
            String s2 = url.toString();
            if ("file".equals(url.getProtocol())) {
                s2 = s2.replace("file:", "file://");
            }
            urlOpen = new String[]{"xdg-open", s2};
        }

        Process process = (Process) Runtime.getRuntime().exec(urlOpen);

        for (String s2 : IOUtils.readLines(process.getErrorStream(), Charset.defaultCharset())) {
            runner.config.LOGGER.error(s2);
        }

        process.getInputStream().close();
        process.getErrorStream().close();
        process.getOutputStream().close();
    }

    /**
     * Creates a listener for an event, this function can be more efficient that running a script file when used properly.
     *
     * @param event
     * @param callback calls your method as a {@link java.util.function.BiConsumer BiConsumer}&lt;{@link BaseEvent}, {@link EventContainer}&gt;
     * @return
     * @see IEventListener
     * @since 1.2.7
     */
    @DocletReplaceTypeParams("E extends keyof Events")
    @DocletReplaceParams("event: E, callback: MethodWrapper<Events[E], EventContainer>")
    public IEventListener on(String event, MethodWrapper<BaseEvent, EventContainer<?>, Object, ?> callback) {
        return on(event, null, false, callback);
    }

    /**
     * Creates a listener for an event, this function can be more efficient that running a script file when used properly.
     *
     * @param event
     * @param callback calls your method as a {@link java.util.function.BiConsumer BiConsumer}&lt;{@link BaseEvent}, {@link EventContainer}&gt;
     * @return
     * @see IEventListener
     * @since 1.9.0
     */
    @DocletReplaceTypeParams("E extends keyof Events")
    @DocletReplaceParams("event: E, joined: boolean, callback: MethodWrapper<Events[E], EventContainer>")
    public IEventListener on(String event, boolean joined, MethodWrapper<BaseEvent, EventContainer<?>, Object, ?> callback) {
        return on(event, null, joined, callback);
    }

    /**
     * Creates a listener for an event, this function can be more efficient that running a script file when used properly.
     *
     * @param event
     * @param callback calls your method as a {@link java.util.function.BiConsumer BiConsumer}&lt;{@link BaseEvent}, {@link EventContainer}&gt;
     * @return
     * @see IEventListener
     * @since 1.9.1
     */
    @DocletReplaceTypeParams("E extends keyof Events")
    @DocletReplaceParams("event: E, filterer: EventFilterer, callback: MethodWrapper<Events[E], EventContainer>")
    public IEventListener on(String event, EventFilterer filterer, MethodWrapper<BaseEvent, EventContainer<?>, Object, ?> callback) {
        return on(event, filterer, false, callback);
    }

    /**
     * Creates a listener for an event, this function can be more efficient that running a script file when used properly.
     *
     * @param event
     * @param callback calls your method as a {@link java.util.function.BiConsumer BiConsumer}&lt;{@link BaseEvent}, {@link EventContainer}&gt;
     * @return
     * @see IEventListener
     * @since 1.9.1
     */
    @DocletReplaceTypeParams("E extends keyof Events")
    @DocletReplaceParams("event: E, filterer: EventFilterer, joined: boolean, callback: MethodWrapper<Events[E], EventContainer>")
    public IEventListener on(String event, EventFilterer filterer, boolean joined, MethodWrapper<BaseEvent, EventContainer<?>, Object, ?> callback) {
        if (callback == null) {
            return null;
        }
        if (!runner.eventRegistry.events.contains(event)) {
            throw new IllegalArgumentException(String.format("Event \"%s\" not found, if it's a custom event register it with 'event.registerEvent()' first.", event));
        }
        if (filterer != null && !filterer.canFilter(event)) {
            throw new IllegalArgumentException(String.format("Provided filterer (%s) cannot be used to filter %s event!", filterer.getClass().getSimpleName(), event));
        }
        Thread th = Thread.currentThread();
        String creatorName = th.getName();
        IEventListener listener = new ScriptEventListener() {

            @Override
            public boolean joined() {
                return joined;
            }

            @Override
            public EventContainer<?> trigger(BaseEvent e) {
                if (filterer != null && !filterer.test(e)) return null;
                EventContainer<?> p = new EventContainer<>(callback.getCtx());
                Thread ot = callback.overrideThread();
                Thread th = runner.threadPool.runTask(() -> {
                    Thread t = Thread.currentThread();
                    t.setName(this.toString());
                    try {
                        callback.accept(e, p);
                    } catch (Throwable ex) {
                        runner.eventRegistry.removeListener(event, this);
                        runner.profile.logError(ex);
                    } finally {
                        p.releaseLock();
                    }
                });
                p.setLockThread(ot == null ? th : ot);
                return p;
            }

            @Override
            public String getCreatorName() {
                return creatorName;
            }

            @Override
            public MethodWrapper<BaseEvent, EventContainer<?>, Object, ?> getWrapper() {
                return callback;
            }

            @Override
            public BaseScriptContext<?> getCtx() {
                return callback.getCtx();
            }

            @Override
            public void off() {
                runner.eventRegistry.removeListener(event, this);
            }

            @Override
            public String toString() {
                return String.format("ScriptEventListener:{\"creator\":\"%s\", \"event\":\"%s\"}", getCreatorName(), event);
            }
        };
        runner.eventRegistry.addListener(event, listener);
        ctx.eventListeners.put(listener, event);
        return listener;
    }

    /**
     * Creates a single-run listener for an event, this function can be more efficient that running a script file when used properly.
     *
     * @param event
     * @param callback calls your method as a {@link java.util.function.BiConsumer BiConsumer}&lt;{@link BaseEvent}, {@link EventContainer}&gt;
     * @return the listener.
     * @see IEventListener
     * @since 1.2.7
     */
    @DocletReplaceTypeParams("E extends keyof Events")
    @DocletReplaceParams("event: E, callback: MethodWrapper<Events[E], EventContainer>")
    public IEventListener once(String event, MethodWrapper<BaseEvent, EventContainer<?>, Object, ?> callback) {
        return once(event, false, callback);
    }


    /**
     * Creates a single-run listener for an event, this function can be more efficient that running a script file when used properly.
     *
     * @param event
     * @param callback calls your method as a {@link java.util.function.BiConsumer BiConsumer}&lt;{@link BaseEvent}, {@link EventContainer}&gt;
     * @return the listener.
     * @see IEventListener
     * @since 1.9.0
     */
    @DocletReplaceTypeParams("E extends keyof Events")
    @DocletReplaceParams("event: E, joined: boolean, callback: MethodWrapper<Events[E], EventContainer>")
    public IEventListener once(String event, boolean joined, MethodWrapper<BaseEvent, EventContainer<?>, Object, ?> callback) {
        if (callback == null) {
            return null;
        }
        if (!runner.eventRegistry.events.contains(event)) {
            throw new IllegalArgumentException(String.format("Event \"%s\" not found, if it's a custom event register it with 'event.registerEvent()' first.", event));
        }
        Thread th = Thread.currentThread();
        String creatorName = th.getName();
        IEventListener listener = new ScriptEventListener() {
            @Override
            public boolean joined() {
                return joined;
            }

            @Override
            public EventContainer<?> trigger(BaseEvent e) {
                runner.eventRegistry.removeListener(event, this);
                EventContainer<?> p = new EventContainer<>(callback.getCtx());
                Thread ot = callback.overrideThread();
                Thread th = runner.threadPool.runTask(() -> {
                    Thread t = Thread.currentThread();

                    t.setName(this.toString());
                    try {
                        callback.accept(e, p);
                    } catch (Throwable ex) {
                        runner.profile.logError(ex);
                    } finally {
                        p.releaseLock();
                    }
                });
                p.setLockThread(ot == null ? th : ot);
                return p;
            }

            @Override
            public String getCreatorName() {
                return creatorName;
            }

            @Override
            public MethodWrapper<BaseEvent, EventContainer<?>, Object, ?> getWrapper() {
                return callback;
            }

            @Override
            public BaseScriptContext<?> getCtx() {
                return callback.getCtx();
            }

            @Override
            public void off() {
                runner.eventRegistry.removeListener(event, this);
            }

            @Override
            public String toString() {
                return String.format("OnceScriptEventListener:{\"creator\":\"%s\", \"event\":\"%s\"}", getCreatorName(), event);
            }

        };
        runner.eventRegistry.addListener(event, listener);
        ctx.eventListeners.put(listener, event);
        return listener;
    }

    /**
     * @param listener
     * @return
     * @see FJsMacros#off(String, IEventListener)
     * @since 1.2.3
     */
    public boolean off(IEventListener listener) {
        return runner.eventRegistry.removeListener(listener);
    }

    /**
     * Removes a {@link IEventListener IEventListener} from an event.
     *
     * @param event
     * @param listener
     * @return
     * @see IEventListener
     * @since 1.2.3
     */
    @DocletReplaceTypeParams("E extends keyof Events")
    @DocletReplaceParams("event: E, listener: IEventListener")
    public boolean off(String event, IEventListener listener) {
        return runner.eventRegistry.removeListener(event, listener);
    }

    /**
     * Will also disable all listeners for the given event, including JsMacros own event listeners.
     *
     * @param event the event to remove all listeners from
     * @since 1.8.4
     */
    @DocletReplaceParams("event: keyof Events")
    public void disableAllListeners(String event) {
        for (IEventListener listener : ImmutableList.copyOf(runner.eventRegistry.getListeners(event))) {
            listener.off();
        }
    }

    /**
     * Will also disable all listeners, including JsMacros own event listeners.
     *
     * @since 1.8.4
     */
    public void disableAllListeners() {
        for (Map.Entry<String, Set<IEventListener>> entry : runner.eventRegistry.getListeners().entrySet()) {
            for (IEventListener listener : ImmutableList.copyOf(entry.getValue())) {
                listener.off();
            }
        }
    }

    /**
     * Will only disable user created event listeners for the given event. This includes listeners
     * created from {@link #on(String, MethodWrapper)}, {@link #once(String, MethodWrapper)},
     * {@link #waitForEvent(String)}, {@link #waitForEvent(String, MethodWrapper)} and
     * {@link #waitForEvent(String, MethodWrapper, MethodWrapper)}.
     * <br>
     * This is the one to reach for in a cleanup routine. It only touches listeners a script made,
     * so the ones JsMacros itself registers, which is what makes the events fire at all, are left
     * alone. {@link #disableAllListeners(String) disableAllListeners} has no such distinction and
     * takes those out as well, so anything that is not a deliberate teardown of the whole event
     * system belongs in this one.
     * <br>
     * The listeners are removed, not disabled, so there is no way to bring them back; a script
     * that wants them again has to subscribe again. Nothing is returned, so
     * {@link #listeners(String) listeners} is the way to find out what was there.
     * example:
     * <pre>
     * // a cleanup routine that takes out its own listeners and nothing else
     * JsMacros.on("Tick", JavaWrapper.methodToJava(function () {
     *   Chat.log("ticking");
     * }));
     * Chat.log(`script listeners on Tick: ${JsMacros.listeners("Tick").size()}`);
     * JsMacros.disableScriptListeners("Tick");
     * Chat.log(`and after the cleanup: ${JsMacros.listeners("Tick").size()}`);
     *
     * // the no argument form does the same for every event at once, and is still
     * // only the script's own listeners
     * JsMacros.disableScriptListeners();
     * </pre>
     *
     * @param event the event to remove all listeners from
     * @since 1.8.4
     */
    @DocletReplaceParams("event: keyof Events")
    public void disableScriptListeners(String event) {
        for (IEventListener listener : ImmutableList.copyOf(runner.eventRegistry.getListeners(event))) {
            if (listener instanceof ScriptEventListener) {
                listener.off();
            }
        }
    }

    /**
     * Will only disable user created event listeners.  This includes listeners created from
     * {@link #on(String, MethodWrapper)}, {@link #once(String, MethodWrapper)},
     * {@link #waitForEvent(String)}, {@link #waitForEvent(String, MethodWrapper)} and
     * {@link #waitForEvent(String, MethodWrapper, MethodWrapper)}.
     *
     * @since 1.8.4
     */
    public void disableScriptListeners() {
        for (Map.Entry<String, Set<IEventListener>> entry : runner.eventRegistry.getListeners().entrySet()) {
            for (IEventListener listener : ImmutableList.copyOf(entry.getValue())) {
                if (listener instanceof ScriptEventListener) {
                    listener.off();
                }
            }
        }
    }

    /**
     * @param event event to wait for
     * @return the event, and a fresh context. This form never joins, so there is
     * nothing on the returned context to release early; use the form taking a
     * {@code join} for that.
     * @throws InterruptedException
     * @since 1.5.0
     */
    @DocletReplaceTypeParams("E extends keyof Events")
    @DocletReplaceParams("event: E")
    @DocletReplaceReturn("FJsMacros$EventAndContext<Events[E]>")
    public EventAndContext<?> waitForEvent(String event) throws InterruptedException {
        return waitForEvent(event, null, null);
    }

    /**
     * @param event event to wait for
     * @return a event and a new context if the event you're waiting for was joined, to leave it early.
     * @throws InterruptedException
     * @since 1.9.0
     */
    @DocletReplaceTypeParams("E extends keyof Events")
    @DocletReplaceParams("event: E, join: boolean")
    @DocletReplaceReturn("FJsMacros$EventAndContext<Events[E]>")
    public EventAndContext<?> waitForEvent(String event, boolean join) throws InterruptedException {
        return waitForEvent(event, join, null, null);
    }

    /**
     * Waits for an event, optionally only one the filter accepts.
     * <br>
     * This blocks the calling thread until an event arrives that the filter returns true for, and
     * there is no timeout, so a filter that never becomes true blocks for as long as the script
     * lives. The filter is run on the event that arrived, so it should be quick, and a filter
     * that throws ends the wait with a {@link RuntimeException}.<br>
     * The listener is registered before the wait starts, so an event arriving in between is not
     * missed, and the event lock the thread was holding is released before it blocks, since
     * holding it would stop the event from ever arriving.
     * example:
     * <pre class="language-typescript">
     * // block until the next block entity update, and only one that is an entity
     * // rather than a plain block changing
     * const result = JsMacros.waitForEvent("BlockUpdate",
     *   JavaWrapper.methodToJava(function (event: Events.BlockUpdate) {
     *     return event.updateType === "ENTITY";
     *   }));
     * const pos = result.event.block.getBlockPos();
     * Chat.log(`a block entity changed at ${pos.getX()}, ${pos.getY()}, ${pos.getZ()}`);
     * // this thread is free again from here, and any event it was bound to is released
     * </pre>
     *
     * @param event event to wait for
     * @param filter accepts the event or not, {@code null} for the first event of any kind
     * @return the event, and a fresh context. This form never joins, so there is
     * nothing on the returned context to release early; use the form taking a
     * {@code join} for that.
     * @throws InterruptedException
     * @since 1.5.0 [citation needed]
     */
    @DocletReplaceTypeParams("E extends keyof Events")
    @DocletReplaceParams("event: E, filter: MethodWrapper<Events[E], undefined, boolean> | null")
    @DocletReplaceReturn("FJsMacros$EventAndContext<Events[E]>")
    public EventAndContext<?> waitForEvent(String event, @Nullable MethodWrapper<BaseEvent, Object, Boolean, ?> filter) throws InterruptedException {
        return waitForEvent(event, filter, null);
    }


    /**
     * @param event
     * @return
     * @throws InterruptedException
     * @since 1.9.0
     */
    @DocletReplaceTypeParams("E extends keyof Events")
    @DocletReplaceParams("event: E, join: boolean, filter: MethodWrapper<Events[E], undefined, boolean> | null")
    @DocletReplaceReturn("FJsMacros$EventAndContext<Events[E]>")
    public EventAndContext<?> waitForEvent(String event, boolean join, @Nullable MethodWrapper<BaseEvent, Object, Boolean, ?> filter) throws InterruptedException {
        return waitForEvent(event, join, filter, null);
    }

    /**
     * waits for an event. if this thread is bound to an event already, this will release current lock.
     *
     * @param event            event to wait for
     * @param filter           filter the event until it has the proper values or whatever.
     * @param runBeforeWaiting runs as a {@link Runnable}, run before waiting, this is a thread-safety thing to prevent "interrupts" from going in between this and things like deferCurrentTask
     * @return the event, and a fresh context. This form never joins, so there is
     * nothing on the returned context to release early; use the form taking a
     * {@code join} for that.
     * @throws InterruptedException
     * @since 1.5.0
     */
    @DocletReplaceTypeParams("E extends keyof Events")
    @DocletReplaceParams("event: E, filter: MethodWrapper<Events[E], undefined, boolean> | null, runBeforeWaiting: MethodWrapper<JavaObject, JavaObject, JavaObject> | null")
    @DocletReplaceReturn("FJsMacros$EventAndContext<Events[E]>")
    public EventAndContext<?> waitForEvent(String event, @Nullable MethodWrapper<BaseEvent, Object, Boolean, ?> filter, @Nullable MethodWrapper<Object, Object, Object, ?> runBeforeWaiting) throws InterruptedException {
        return waitForEvent(event, false, filter, runBeforeWaiting);
    }



    /**
     * waits for an event. if this thread is bound to an event already, this will release current lock.
     * <br>
     * This is the full form of the wait and the only one of the six that takes every argument. It
     * blocks the calling thread until an event arrives that the filter accepts, and there is no
     * timeout, so a filter that never becomes true blocks for as long as the script lives. A filter
     * that throws is wrapped in a {@link RuntimeException} and ends the wait.
     * <br>
     * The listener is registered before the wait starts rather than after, so an event that
     * arrives in between is not missed, and the event lock the thread was holding is released
     * before it blocks, since holding it would stop the event from ever arriving. That means
     * anything the thread was still doing in an event handler is left unfinished until the wait
     * returns, which is why a script that needs to do something that waits is better written as a
     * {@link #runScript(String, BaseEvent, MethodWrapper) runScript} with a callback.
     * <br>
     * The {@code join} argument is what decides whether the event system waits on the listener
     * this registers. It only has an effect on an event that is joinable, and the joinable ones
     * are exactly the cancellable ones; on any other event the listener is called and what it
     * hands back is thrown away, so {@code true} there means the same as {@code false}. Nothing
     * is marked joinable on its own, so this is currently a way to hold a cancellable event such
     * as a chat message or an inventory click open while a script handles it.<br>
     * What comes back is an {@link EventAndContext}, which holds the event itself and the context
     * it ran on. The context is how a joined event is let go of early, by calling
     * {@link EventContainer#releaseLock()} on it, rather than waiting for the handler to finish.
     * example:
     * <pre class="language-typescript">
     * // the full form: joined, filtered, with nothing to run before the wait.
     * // join only does anything on a joinable event, and SendMessage is a
     * // cancellable one, so the client is held on the event until the lock is
     * // released
     * const result = JsMacros.waitForEvent("SendMessage", true,
     *   JavaWrapper.methodToJava(function (event: Events.SendMessage) {
     *     return event.message !== null;
     *   }), null);
     * if (result.event.message !== null) {
     *   result.event.message = `say ${result.event.message}`;
     * }
     * // the chat screen is still waiting on the join, and this is what lets it
     * // carry on and send what the event says now
     * result.context.releaseLock();
     * </pre>
     *
     * @param event            event to wait for
     * @param join             whether the event system waits on this listener, which only happens for a joinable event
     * @param filter           filter the event until it has the proper values or whatever.
     * @param runBeforeWaiting runs as a {@link Runnable}, run before waiting, this is a thread-safety thing to prevent "interrupts" from going in between this and things like deferCurrentTask
     * @return a event and a new context if the event you're waiting for was joined, to leave it early.
     * @throws InterruptedException
     * @since 1.9.0
     */
    @DocletReplaceTypeParams("E extends keyof Events")
    @DocletReplaceParams("event: E, join: boolean, filter: MethodWrapper<Events[E], undefined, boolean> | null, runBeforeWaiting: MethodWrapper<JavaObject, JavaObject, JavaObject> | null")
    @DocletReplaceReturn("FJsMacros$EventAndContext<Events[E]>")
    public EventAndContext<?> waitForEvent(String event, boolean join, @Nullable MethodWrapper<BaseEvent, Object, Boolean, ?> filter, @Nullable MethodWrapper<Object, Object, Object, ?> runBeforeWaiting) throws InterruptedException {
        // event return values
        final BaseEvent[] ev = {null};
        // create a new event container so we can actually release joined events
        EventContainer<?>[] ctxCont = new EventContainer[]{new EventContainer<>(ctx)};
        ctx.wrapSleep(() -> {
            if (!runner.eventRegistry.events.contains(event)) {
                throw new IllegalArgumentException(String.format("Event \"%s\" not found, if it's a custom event register it with 'event.registerEvent()' first.", event));
            }

            //get current thread establish the lock to use for waiting blah blah blah
            Thread th = Thread.currentThread();
            String creatorName = th.getName();
            Semaphore lock = new Semaphore(0);
            Semaphore lock2 = new Semaphore(0);

            boolean[] done = new boolean[]{false};

            // create the listener
            IEventListener listener = new ScriptEventListener() {
                @Override
                public boolean joined() {
                    return join;
                }

                @Override
                public EventContainer<?> trigger(BaseEvent evt) {
                    ev[0] = evt;
                    // allow for initial thread to run its filter
                    lock.release();
                    try {
                        // wait for filter to finish
                        lock2.acquire();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return null;
                    }
                    // if filter done, we can remove self and return the event context
                    if (done[0]) {
                        runner.eventRegistry.removeListener(event, this);
                        ctx.bindEvent(th, (EventContainer) ctxCont[0]);
                        return ctxCont[0];
                    }
                    return null;
                }

                @Override
                public String getCreatorName() {
                    return creatorName;
                }

                @Override
                public MethodWrapper<BaseEvent, EventContainer<?>, Object, ?> getWrapper() {
                    return null;
                }

                @Override
                public BaseScriptContext<?> getCtx() {
                    return ctx;
                }

                @Override
                public void off() {
                    runner.eventRegistry.removeListener(event, this);
                    th.interrupt();
                }

                @Override
                public String toString() {
                    return String.format("WaitForEventListener:{\"creator\":\"%s\", \"event\":\"%s\"}", th.getName(), event);
                }
            };
            // register the listener
            runner.eventRegistry.addListener(event, listener);
            ctx.eventListeners.put(listener, event);

            // run before, this is a thread-safety thing to prevent "interrupts" from going in between this and things like deferCurrentTask
            // it is thread safe because we already registered the listener so we won't miss any events
            if (runBeforeWaiting != null) {
                runBeforeWaiting.run();
            }

            // make sure the current context isn't still locked.
            ctx.releaseBoundEventIfPresent(th);

            // set the new EventContainer's lock
            ctxCont[0].setLockThread(th);

            // waits for event
            while (!done[0]) {
                lock.acquire();
                try {
                    // check the filter
                    done[0] = filter == null || filter.test(ev[0]);
                } catch (Throwable ex) {
                    runner.eventRegistry.removeListener(event, listener);
                    throw new RuntimeException("Error thrown in filter", ex);
                } finally {
                    lock2.release();
                }
            }

        });
        // returns new context and event value to the user so they can release joined stuff early
        return new EventAndContext<>(ev[0], ctxCont[0]);
    }

    /**
     * @param event
     * @return a list of script-added listeners.
     * @since 1.2.3
     */
    @DocletReplaceParams("event: keyof Events")
    public List<IEventListener> listeners(String event) {
        List<IEventListener> listeners = new ArrayList<>();
        for (IEventListener l : runner.eventRegistry.getListeners(event)) {
            if (!(l instanceof BaseListener)) {
                listeners.add(l);
            }
        }
        return listeners;
    }

    /**
     * create an event filterer.<br>
     * this exists to reduce lag when listening to frequently triggered events.
     * <br>
     * A filterer is a separate object with its own setters, one per event, rather than a
     * callback. The event system asks it whether an event is worth passing on before the listener
     * is run at all, so the work of throwing an event away never reaches the script. The filterer
     * is built once and consulted for every event, so its settings are state that a listener
     * should not be changing from inside itself.<br>
     * The setters return the filterer, so they chain. Which events can be filtered at all is
     * fixed by which ones have a filterer class, and asking for one that does not is an
     * {@link IllegalArgumentException}, as is asking for an event name that is not registered.
     * {@link #createModulusEventFilterer(int) createModulusEventFilterer} and
     * {@link #invertEventFilterer(EventFilterer) invertEventFilterer} work on any event at all,
     * since neither looks at what the event is.
     * example:
     * <pre>
     * // a block update filterer, narrowed to one position and one block id. the
     * // event system asks it about every incoming update, so only the one wanted
     * // ever reaches the listener
     * const near = JsMacros.createEventFilterer("BlockUpdate")
     *   .setPos(0, -60, 0)
     *   .setBlockId("minecraft:chest");
     * JsMacros.on("BlockUpdate", near, JavaWrapper.methodToJava(function (event) {
     *   const pos = event.block.getBlockPos();
     *   Chat.log(`the chest at ${pos.getX()}, ${pos.getY()}, ${pos.getZ()} changed`);
     * }));
     *
     * // the same filterer can be re-pointed later, and the listener does not have
     * // to be touched to do it
     * near.setBlockId("minecraft:furnace").setUpdateType("ENTITY");
     *
     * // and a filterer that works on any event at all
     * JsMacros.on("Tick", JsMacros.createModulusEventFilterer(20),
     *   JavaWrapper.methodToJava(function () {
     *     Chat.log("about a second went by");
     *   }));
     * </pre>
     * @param event the name of the event to build a filterer for
     * @return a filterer for that event, with its own setters for the event's fields
     * @throws IllegalArgumentException if the event is not registered, or is registered but has
     *         no filterer class
     * @since 1.9.1
     */
    @DocletReplaceTypeParams("E extends keyof EventFilterers")
    @DocletReplaceParams("event: E")
    @DocletReplaceReturn("EventFilterers[E]")
    public EventFilterer createEventFilterer(String event) {
        Class<? extends EventFilterer> fclass = runner.eventRegistry.filterableEvents.get(event);
        if (fclass == null) {
            if (runner.eventRegistry.events.contains(event)) {
                throw new IllegalArgumentException(String.format("Event %s doesn't have a filterer class!", event));
            } else {
                throw new IllegalArgumentException(String.format("Event %s not found!", event));
            }
        }
        try {
            return fclass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * create a composed event filterer.<br>
     * this filterer combines multiple filterers together with and/or logic.
     * @since 1.9.1
     */
    public FiltererComposed createComposedEventFilterer(EventFilterer initial) {
        return new FiltererComposed(initial);
    }

    /**
     * create a modulus event filterer.<br>
     * this filterer only let every nth event pass through.
     * @since 1.9.1
     */
    public FiltererModulus createModulusEventFilterer(int quotient) {
        return new FiltererModulus(quotient);
    }

    /**
     * inverts the base filterer's result.<br>
     * this checks if the base is already inverted.<br>
     * e.g. {@code filterer == invert(invert(filterer))} would be {@code true}.
     * @since 1.9.1
     */
    public EventFilterer invertEventFilterer(EventFilterer base) {
        return FiltererInverted.invert(base);
    }

    /**
     * create a custom event object that can trigger a event. It's recommended to use
     * {@link EventCustom#registerEvent()} to set up the event to be visible in the GUI.
     * <br>
     * This is how a script talks to another script: the object created here is the same kind of
     * thing the event system hands to a listener, with its own {@code put} and {@code get} calls
     * to carry values along. Nothing is sent anywhere by creating it, and nothing is sent by the
     * {@code put} calls either. The event goes out on
     * {@link EventCustom#trigger() trigger()}, so a listener registered under the same name has
     * to already be in place before that is called or nothing will hear it.
     * <br>
     * The name is not in the generated script type definitions, since those are built from the
     * event classes in the source, so a script that both fires and listens for a custom event
     * will not type-check against them without a cast.
     * example:
     * <pre>
     * // carry a few values along with the event rather than through a global
     * const ping = JsMacros.createCustomEvent("MyPluginPing");
     * ping.putString("from", "my-plugin");
     * ping.putInt("count", 1);
     * // registerEvent is what makes the name show up in the GUI's event picker, and
     * // without it nothing can subscribe to the name at all
     * ping.registerEvent();
     * // trigger is the part that actually sends it, and it is a separate step on purpose
     * ping.trigger();
     *
     * // the same object doubles as the payload for a script run in its own context,
     * // which is the way to hand values to a script that waits for them
     * const data = JsMacros.createCustomEvent("MyPluginJob");
     * data.putString("task", "rebuild-world");
     * data.putBoolean("urgent", true);
     * JsMacros.runScript("worker.js", data);
     * </pre>
     *
     * @param eventName name of the event. please don't use an existing one... your scripts might not like that.
     * @return the custom event, which carries the values and is what gets triggered.
     * @see BaseEventRegistry#addEvent(String)
     * @since 1.2.8
     */
    public EventCustom createCustomEvent(String eventName) {
        return new EventCustom(runner, eventName);
    }

    /**
     * asserts if {@code event} is the correct type of event<br>
     * and convert {@code event} type to target type in ts<br>
     * example:
     * <pre>
     * JsMacros.assertEvent(event, 'Service')
     * </pre>
     * @param event the event to assert
     * @param type string of the event type
     * @since 1.9.0
     */
    @DocletReplaceTypeParams("E extends keyof Events")
    @DocletReplaceParams("event: Events.BaseEvent, type: E")
    @DocletReplaceReturn("asserts event is Events[E]")
    public void assertEvent(BaseEvent event, String type) {
        if (event == null) throw new AssertionError("event is null!");
        if (type == null) throw new AssertionError("event type is null!");
        if (event.getClass().isAnnotationPresent(Event.class)) {
            String eventName = event.getClass().getAnnotation(Event.class).value();
            if (!eventName.equals(type)) {
                throw new AssertionError(String.format("event type (%s) is not %s!", eventName, type));
            }
        } else {
            throw new AssertionError("The event doesn't have proper event annotation, " + event.getClass().getSimpleName());
        }
    }

    public interface ScriptEventListener extends IEventListener {
        String getCreatorName();

        @Nullable
        MethodWrapper<BaseEvent, EventContainer<?>, Object, ?> getWrapper();

        BaseScriptContext<?> getCtx();
    }

    public static class EventAndContext<E extends BaseEvent> {
        public final E event;
        public final EventContainer<?> context;

        public EventAndContext(E event, EventContainer<?> context) {
            this.event = event;
            this.context = context;
        }

        public String toString() {
            return String.format("EventAndContext:{\"event\": %s, \"context\": %s}", event.toString(), context.toString());
        }

    }

}
