package com.jsmacrosce.jsmacros.core.library.impl;

import com.google.common.collect.ImmutableList;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
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
 * Script library for event listeners, script execution, and profile services.
 * <p>
 * Scripts access this library as {@code JsMacros} for event handling, script execution, and
 * profile services.
 * <pre>
 * const listener = JsMacros.on("Tick", JavaWrapper.methodToJava(function (event) {
 *   Chat.log(event.getEventName());
 * }));
 * JsMacros.off(listener);
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

    /**
     * Binds this library instance to a script context and its core.
     * Scripts normally use the global {@code JsMacros} library rather than constructing another instance.
     *
     * @param context script context whose core this library uses; must not be {@code null}
     */
    public FJsMacros(BaseScriptContext<?> context) {
        super(context);
    }

    /**
     * @return this script's profile.
     */
    public BaseProfile getProfile() {
        return runner.profile;
    }

    /**
     * @return this script's configuration manager.
     */
    public ConfigManager getConfig() {
        return runner.config;
    }

    /**
     * Returns the manager for this profile's background services.
     *
     * @return the service manager
     * @since 1.6.3
     */
    public ServiceManager getServiceManager() {
        return runner.services;
    }

    /**
     * @return an immutable snapshot of currently open script contexts
     * @since 1.4.0
     */
    public List<BaseScriptContext<?>> getOpenContexts() {
        return ImmutableList.copyOf(runner.getContexts());
    }

    /**
     * Runs a script file without supplying an event or completion callback.
     *
     * @param file path of the script file
     * @return the container for the running script
     * @since 1.1.5
     */
    public EventContainer<?> runScript(String file) {
        return runScript(file, (EventCustom) null, null);
    }

    /**
     * Runs a script file with the supplied event as its trigger.
     *
     * @param file path of the script file
     * @param fakeEvent event supplied to the script, or {@code null}
     * @return the container for the running script
     * @since 1.6.3
     */
    public EventContainer<?> runScript(String file, @Nullable BaseEvent fakeEvent) {
        return runScript(file, fakeEvent, null);
    }

    /**
     * Runs a script file with an event and optional completion callback.
     *
     * @param file path of the script file
     * @param fakeEvent event supplied to the script, or {@code null}
     * @param callback called when the script completes, or {@code null}
     * @return the container for the running script
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
     * Runs a script string in the specified language without a completion callback.
     *
     * @param language script language
     * @param script script source
     * @return the container for the running script
     * @since 1.2.4
     */
    public EventContainer<?> runScript(String language, String script) {
        return runScript(language, script, null);
    }

    /**
     * Runs a script string in the specified language, optionally calling back when it completes.
     *
     * @param language script language
     * @param script script source
     * @param callback completion callback, or {@code null}
     * @return the container for the running script
     * @since 1.2.4
     */
    public EventContainer<?> runScript(String language, String script, @Nullable MethodWrapper<Throwable, Object, Object, ?> callback) {
        return runScript(language, script, null, callback);
    }

    /**
     * Runs script source in the specified language, optionally using a file path as its source file and calling back on completion.
     * The file path is resolved relative to this script's folder; {@code null} means no file path is supplied.
     *
     * @param language script language
     * @param script script source
     * @param file optional path relative to this script's folder
     * @param callback completion callback, or {@code null}
     * @return the container for the running script
     * @since 1.6.0
     */
    public EventContainer<?> runScript(String language, String script, @Nullable String file, @Nullable MethodWrapper<Throwable, Object, Object, ?> callback) {
        return runScript(language, script, file, null, callback);
    }

    /**
     * Runs script source in the specified language with an optional file path, event, and completion callback.
     * The file path is resolved relative to this script's folder; {@code null} omits it. The event is supplied to the script.
     *
     * @param language script language
     * @param script script source
     * @param file optional path relative to this script's folder
     * @param event event supplied to the script, or {@code null}
     * @param callback completion callback, or {@code null}
     * @return the container for the running script
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
     * Creates a callback that runs the script file for each invocation, passing the triggering event to it.
     * The file path is resolved from the process working directory.
     *
     * @param file script file path
     * @param <T> callback input type
     * @param <U> callback context type
     * @param <R> callback result type
     * @return the wrapped script callback
     * @since 1.7.0
     */
    public <T, U, R> MethodWrapper<T, U, R, ?> wrapScriptRun(String file) {
        return new WrappedScript<>(runner, (e) -> (EventContainer<BaseScriptContext<?>>) runner.exec(new ScriptTrigger(ScriptTrigger.TriggerType.EVENT, e.getEventName(), Path.of(file), true, false), e, null, null), false);
    }

    /**
     * Creates a callback that runs the script source for each invocation, passing the triggering event to it.
     *
     * @param language script language
     * @param script script source
     * @param <T> callback input type
     * @param <U> callback context type
     * @param <R> callback result type
     * @return the wrapped script callback
     * @since 1.7.0
     */
    public <T, U, R> MethodWrapper<T, U, R, ?> wrapScriptRun(String language, String script) {
        return new WrappedScript<>(runner, (e) -> (EventContainer<BaseScriptContext<?>>) runner.exec(language, script, null, e, null, null), false);
    }

    /**
     * Creates a callback that runs the script source for each invocation, passing the triggering event to it.
     * The optional file path is resolved relative to this script's folder.
     *
     * @param language script language
     * @param script script source
     * @param file optional path relative to this script's folder
     * @param <T> callback input type
     * @param <U> callback context type
     * @param <R> callback result type
     * @return the wrapped script callback
     * @since 1.7.0
     */
    public <T, U, R> MethodWrapper<T, U, R, ?> wrapScriptRun(String language, String script, @Nullable String file) {
        return new WrappedScript<>(runner, (e) -> (EventContainer<BaseScriptContext<?>>) runner.exec(language, script, file != null ? ctx.getContainedFolder().toPath().resolve(file).toFile() : null, e, null, null), false);
    }

    /**
     * Creates an asynchronous callback that runs the script file for each invocation, passing the triggering event to it.
     * The file path is resolved from the process working directory.
     *
     * @param file script file path
     * @param <T> callback input type
     * @param <U> callback context type
     * @param <R> callback result type
     * @return the wrapped asynchronous script callback
     * @since 1.7.0
     */
    public <T, U, R> MethodWrapper<T, U, R, ?> wrapScriptRunAsync(String file) {
        return new WrappedScript<>(runner, (e) -> (EventContainer<BaseScriptContext<?>>) runner.exec(new ScriptTrigger(ScriptTrigger.TriggerType.EVENT, e.getEventName(), Path.of(file), true, false), e, null, null), true);
    }

    /**
     * Creates an asynchronous callback that runs the script source for each invocation, passing the triggering event to it.
     *
     * @param language script language
     * @param script script source
     * @param <T> callback input type
     * @param <U> callback context type
     * @param <R> callback result type
     * @return the wrapped asynchronous script callback
     * @since 1.7.0
     */
    public <T, U, R> MethodWrapper<T, U, R, ?> wrapScriptRunAsync(String language, String script) {
        return new WrappedScript<>(runner, (e) -> (EventContainer<BaseScriptContext<?>>) runner.exec(language, script, null, e, null, null), true);
    }

    /**
     * Creates an asynchronous callback that runs the script source for each invocation, passing the triggering event to it.
     * The optional file path is resolved relative to this script's folder.
     *
     * @param language script language
     * @param script script source
     * @param file optional path relative to this script's folder
     * @param <T> callback input type
     * @param <U> callback context type
     * @param <R> callback result type
     * @return the wrapped asynchronous script callback
     * @since 1.7.0
     */
    public <T, U, R> MethodWrapper<T, U, R, ?> wrapScriptRunAsync(String language, String script, @Nullable String file) {
        return new WrappedScript<>(runner, (e) -> (EventContainer<BaseScriptContext<?>>) runner.exec(language, script, file != null ? ctx.getContainedFolder().toPath().resolve(file).toFile() : null, e, null, null), true);
    }

    // TODO: Migrate open and openUrl to client-only Utils library and deprecate these methods.
    /**
     * Opens a file with the default system program.
     *
     * @param path path resolved relative to the current script's folder
     * @since 1.1.8
     */
    @DocletIgnore
    @Deprecated
    public void open(String path) throws IOException {
        openUrl(ctx.getContainedFolder().toPath().resolve(path).toUri().toURL());
    }

    /**
     * Opens the URL with the operating system's default handler.
     *
     * @param url URL string to open
     * @since 1.6.0
     */
    @DocletIgnore
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
     * Registers a listener for an event.
     *
     * @param event registered event name; script subscriptions are not remapped from old aliases
     * @param callback calls your method as a {@link java.util.function.BiConsumer BiConsumer}&lt;{@link BaseEvent}, {@link EventContainer}&gt;
     * @return the registered listener, or {@code null} if the callback is {@code null}
     * @see IEventListener
     * @since 1.2.7
     */
    @DocletReplaceTypeParams("E extends keyof Events")
    @DocletReplaceParams("event: E, callback: MethodWrapper<Events[E], EventContainer>")
    public IEventListener on(String event, MethodWrapper<BaseEvent, EventContainer<?>, Object, ?> callback) {
        return on(event, null, false, callback);
    }

    /**
     * Registers a listener for an event; {@code joined} requests a joined callback when the event supports joining.
     *
     * @param event registered event name; script subscriptions are not remapped from old aliases
     * @param joined whether to request joined handling for a joinable event
     * @param callback calls your method as a {@link java.util.function.BiConsumer BiConsumer}&lt;{@link BaseEvent}, {@link EventContainer}&gt;
     * @return the registered listener, or {@code null} if the callback is {@code null}
     * @see IEventListener
     * @since 1.9.0
     */
    @DocletReplaceTypeParams("E extends keyof Events")
    @DocletReplaceParams("event: E, joined: boolean, callback: MethodWrapper<Events[E], EventContainer>")
    public IEventListener on(String event, boolean joined, MethodWrapper<BaseEvent, EventContainer<?>, Object, ?> callback) {
        return on(event, null, joined, callback);
    }

    /**
     * Registers a listener for an event, optionally filtering which events reach the callback.
     *
     * @param event registered event name; script subscriptions are not remapped from old aliases
     * @param filterer filterer that must support this event; {@code null} accepts events without filtering
     * @param callback calls your method as a {@link java.util.function.BiConsumer BiConsumer}&lt;{@link BaseEvent}, {@link EventContainer}&gt;
     * @return the registered listener, or {@code null} if the callback is {@code null}
     * @see IEventListener
     * @since 1.9.1
     */
    @DocletReplaceTypeParams("E extends keyof Events")
    @DocletReplaceParams("event: E, filterer: EventFilterer, callback: MethodWrapper<Events[E], EventContainer>")
    public IEventListener on(String event, EventFilterer filterer, MethodWrapper<BaseEvent, EventContainer<?>, Object, ?> callback) {
        return on(event, filterer, false, callback);
    }

    /**
     * Registers a listener for an event, optionally filtering which events reach the callback; {@code joined} requests joined handling when supported.
     *
     * @param event registered event name; script subscriptions are not remapped from old aliases
     * @param filterer filterer that must support this event; {@code null} accepts events without filtering
     * @param joined whether to request joined handling for a joinable event
     * @param callback calls your method as a {@link java.util.function.BiConsumer BiConsumer}&lt;{@link BaseEvent}, {@link EventContainer}&gt;
     * @return the registered listener, or {@code null} if the callback is {@code null}
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
                        if (e.cancellable() || joined) {
                            callback.apply(e, p);
                        } else {
                            callback.accept(e, p);
                        }
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
            public void triggerInline(BaseEvent e) {
                if (filterer != null && !filterer.test(e)) return;
                EventContainer<?> container = new EventContainer<>(callback.getCtx());
                try {
                    if (e.cancellable()) {
                        callback.apply(e, container);
                    } else {
                        callback.accept(e, container);
                    }
                } catch (Throwable ex) {
                    runner.eventRegistry.removeListener(event, this);
                    runner.profile.logError(ex);
                } finally {
                    container.releaseLock();
                }
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
     * Registers a listener that is removed when the event triggers.
     *
     * @param event registered event name; script subscriptions are not remapped from old aliases
     * @param callback calls your method as a {@link java.util.function.BiConsumer BiConsumer}&lt;{@link BaseEvent}, {@link EventContainer}&gt;
     * @return the registered listener, or {@code null} if the callback is {@code null}
     * @see IEventListener
     * @since 1.2.7
     */
    @DocletReplaceTypeParams("E extends keyof Events")
    @DocletReplaceParams("event: E, callback: MethodWrapper<Events[E], EventContainer>")
    public IEventListener once(String event, MethodWrapper<BaseEvent, EventContainer<?>, Object, ?> callback) {
        return once(event, false, callback);
    }


    /**
     * Registers a listener that is removed when the event triggers; {@code joined} requests joined handling when supported.
     *
     * @param event registered event name; script subscriptions are not remapped from old aliases
     * @param joined whether to request joined handling for a joinable event
     * @param callback calls your method as a {@link java.util.function.BiConsumer BiConsumer}&lt;{@link BaseEvent}, {@link EventContainer}&gt;
     * @return the registered listener, or {@code null} if the callback is {@code null}
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
                        if (e.cancellable() || joined) {
                            callback.apply(e, p);
                        } else {
                            callback.accept(e, p);
                        }
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
            public void triggerInline(BaseEvent e) {
                runner.eventRegistry.removeListener(event, this);
                EventContainer<?> container = new EventContainer<>(callback.getCtx());
                try {
                    if (e.cancellable()) {
                        callback.apply(e, container);
                    } else {
                        callback.accept(e, container);
                    }
                } catch (Throwable ex) {
                    runner.profile.logError(ex);
                } finally {
                    container.releaseLock();
                }
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
     * @param listener listener to remove
     * @return {@code true} if it was removed from an event, or {@code false} if it was not registered
     * @see FJsMacros#off(String, IEventListener)
     * @since 1.2.3
     */
    public boolean off(IEventListener listener) {
        return runner.eventRegistry.removeListener(listener);
    }

    /**
     * Removes a {@link IEventListener IEventListener} from an event.
     *
     * @param event event name under which the listener is registered
     * @param listener listener to remove from that event
     * @return {@code true} if it was removed from that event, or {@code false} if it was not registered there
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
     * Removes script-created listeners for the given event, leaving built-in listeners untouched.
     * Use {@link #disableAllListeners(String)} to remove both kinds.
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
     * Removes script-created listeners for all events, leaving built-in listeners untouched.
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
     * Waits for the next event of the given name.
     * @param event registered event name
     * @return the event and its context. This form requests no explicit join, but cancellable
     * events still join. Release the returned context's lock promptly after processing a held event.
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
     * Waits for the next event, optionally requesting a join when the event supports it.
     * @param event registered event name
     * @param join whether to request joining a joinable event; cancellable events join regardless
     * @return the event and its context; release the context lock promptly if the event is held
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
     * Waits until an event arrives for which the optional filter returns true. The listener is
     * registered before waiting, and any currently bound event lock is released before blocking.
     * There is no timeout; a filter exception ends the wait with a {@link RuntimeException}.
     *
     * @param event registered event name
     * @param filter accepts an event, or {@code null} to accept the first event
     * @return the event and its context. This form requests no explicit join, but cancellable
     * events still join. Release the returned context's lock promptly after processing a held event.
     * @throws InterruptedException
     * @since 1.5.0
     */
    @DocletReplaceTypeParams("E extends keyof Events")
    @DocletReplaceParams("event: E, filter: MethodWrapper<Events[E], undefined, boolean> | null")
    @DocletReplaceReturn("FJsMacros$EventAndContext<Events[E]>")
    public EventAndContext<?> waitForEvent(String event, @Nullable MethodWrapper<BaseEvent, Object, Boolean, ?> filter) throws InterruptedException {
        return waitForEvent(event, filter, null);
    }


    /**
     * @param event registered event name
     * @param join whether to request joining a joinable event; cancellable events join regardless
     * @param filter accepts an event, or {@code null} to accept the first event
     * @return the event and its context; release the context lock promptly if the event is held
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
     * Waits until an event arrives for which the optional filter returns true. Registers its listener
     * before running {@code runBeforeWaiting}, then releases any currently bound event lock before waiting.
     *
     * @param event registered event name
     * @param filter accepts an event, or {@code null} to accept the first event
     * @param runBeforeWaiting callback run after listener registration and before waiting
     * @return the event and its context. This form requests no explicit join, but cancellable
     * events still join. Release the returned context's lock promptly after processing a held event.
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
     * Waits until an event passes the optional filter. The listener is registered before
     * {@code runBeforeWaiting} runs; any currently bound event lock is released before waiting.
     * There is no timeout, and filter exceptions are wrapped in a {@link RuntimeException}.
     * Cancellable events join regardless of {@code join}; other events join only when requested
     * and joinable. Release the returned context's lock promptly when the event is held.
     *
     * @param event registered event name
     * @param join whether to request joining a joinable event; cancellable events join regardless
     * @param filter accepts an event, or {@code null} to accept the first event
     * @param runBeforeWaiting callback run after listener registration and before waiting
     * @return the event and its context
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
     * @param event event name
     * @return listeners for this event other than built-in {@code BaseListener} instances
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
     * Creates a filterer for an event with a registered filterer class. Filterers are checked
     * before the listener callback runs; available fields and setters depend on the event.
     * Throws {@link IllegalArgumentException} if the event is unregistered or has no filterer.
     * <pre>
     * // Narrow a block-update listener to a position and block id.
     * const near = JsMacros.createEventFilterer("BlockUpdate")
     *   .setPos(0, -60, 0)
     *   .setBlockId("minecraft:chest");
     * JsMacros.on("BlockUpdate", near, JavaWrapper.methodToJava(function (event) {
     *   const pos = event.block.getBlockPos();
     *   Chat.log(`the chest at ${pos.getX()}, ${pos.getY()}, ${pos.getZ()} changed`);
     * }));
     *
     * // Change the filter without replacing the listener.
     * near.setBlockId("minecraft:furnace").setUpdateType("ENTITY");
     *
     * // A modulus filterer can be used with any event.
     * JsMacros.on("Tick", JsMacros.createModulusEventFilterer(20),
     *   JavaWrapper.methodToJava(function () {
     *     Chat.log("about a second went by");
     *   }));
     * </pre>
     * @param event registered event name with an available filterer class
     * @return a new filterer for the event
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
     * Creates a filterer that combines conditions as OR groups of AND conditions; use its
     * {@code and} and {@code or} methods to add conditions.
     * @param initial initial filterer in the first AND group
     * @return a composed filterer initialized with {@code initial}
     * @since 1.9.1
     */
    public FiltererComposed createComposedEventFilterer(EventFilterer initial) {
        return new FiltererComposed(initial);
    }

    /**
     * Creates a stateful filterer that passes every {@code quotient}th filter call; a fresh
     * filterer rejects calls until the threshold is reached. The count is shared by listeners
     * using the same filterer. The constructor applies {@link Math#abs(int)}; zero and
     * {@code Integer.MIN_VALUE} make every call pass.
     * @param quotient threshold in filter calls; its absolute value is used except for
     *                  {@code Integer.MIN_VALUE}
     * @return the new modulus filterer
     * @since 1.9.1
     */
    public FiltererModulus createModulusEventFilterer(int quotient) {
        return new FiltererModulus(quotient);
    }

    /**
     * Inverts a filterer's test result; inverting an already inverted filterer returns its base.
     * Throws {@link IllegalArgumentException} if {@code base} is {@code null}.
     * @param base filterer to invert
     * @return the inverted filterer, or its base if already inverted
     * @since 1.9.1
     */
    public EventFilterer invertEventFilterer(EventFilterer base) {
        return FiltererInverted.invert(base);
    }

    /**
     * Creates a named event that can carry key-value arguments. Call
     * {@link EventCustom#registerEvent()} to register its name for subscription and GUI visibility,
     * then register listeners before calling {@link EventCustom#trigger()}, which dispatches
     * immediately without queuing. Creation and storing arguments do not dispatch.
     * <pre>
     * // Store arguments, register the name, then dispatch.
     * const ping = JsMacros.createCustomEvent("MyPluginPing");
     * ping.putString("from", "my-plugin");
     * ping.putInt("count", 1);
     * ping.registerEvent();
     * ping.trigger();
     *
     * // Custom events can also be passed as script arguments.
     * const data = JsMacros.createCustomEvent("MyPluginJob");
     * data.putString("task", "rebuild-world");
     * data.putBoolean("urgent", true);
     * JsMacros.runScript("worker.js", data);
     * </pre>
     *
     * @param eventName name used to dispatch the event
     * @return the event object
     * @since 1.2.8
     */
    public EventCustom createCustomEvent(String eventName) {
        return new EventCustom(runner, eventName);
    }

    /**
     * Throws {@link AssertionError} unless the event's {@link Event} annotation value equals
     * {@code type}; also narrows the event type in generated TypeScript declarations.
     * @param event event to check
     * @param type expected event name
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

    /**
     * Listener implementation used for script event subscriptions, rather than a global library.
     * <pre>
     * const listener = JsMacros.on("Tick", JavaWrapper.methodToJava(function () {}));
     * try {
     *   Chat.log(listener.getCreatorName());
     * } finally {
     *   JsMacros.off(listener);
     * }
     * </pre>
     */
    public interface ScriptEventListener extends IEventListener {
        /**
         * Triggers this listener through its inline hook; the default delegates to {@code trigger} and discards its returned container.
         * This default does not ensure inline execution or completion; implementations may override it.
         * @param event event occurrence passed to this listener
         */
        default void triggerInline(BaseEvent event) {
            trigger(event);
        }
        /** Returns the recorded creator label, not necessarily the name of the currently executing thread.
         * Listeners from {@code on}, {@code once} and {@code waitForEvent} record the registration thread's name.
         * @return creator label recorded for this listener
         */
        String getCreatorName();

        /**
         * Returns the callback wrapper associated with this listener, if present.
         * Listeners created by {@code waitForEvent} have no callback wrapper.
         * @return callback wrapper, or {@code null} when absent
         */
        @Nullable
        MethodWrapper<BaseEvent, EventContainer<?>, Object, ?> getWrapper();

        /** Returns the script context associated with this listener.
         * Listeners from {@code on} and {@code once} use the callback wrapper's context,
         * which need not be the registration context; {@code waitForEvent} listeners use the waiting context.
         * @return associated script context
         */
        BaseScriptContext<?> getCtx();
    }

    /**
     * Pairs an event received by {@code waitForEvent} with its event container.
     * <pre>
     * const next = JsMacros.waitForEvent("Tick");
     * try {
     *   Chat.log(next.event.getEventName());
     * } finally {
     *   next.context.releaseLock();
     * }
     * </pre>
     * Release the context lock promptly after processing when the returned event is held; the result is not necessarily currently locked.
     *
     * @param <E> paired event type
     */
    public static class EventAndContext<E extends BaseEvent> {
        /** The event reference paired with {@code context}; it is not copied or frozen. */
        public final E event;
        /** The event container paired with {@code event}; release its lock promptly when processing a held event. */
        public final EventContainer<?> context;

        /** Stores the supplied event and container references without dispatching or changing lock state.
         * @param event event to pair
         * @param context paired event container; supply a non-null reference for {@code toString()}
         */
        public EventAndContext(E event, EventContainer<?> context) {
            this.event = event;
            this.context = context;
        }

        /** Formats both stored references as {@code EventAndContext:{"event": %s, "context": %s}} using their {@code toString()} results; this is diagnostic text, not JSON or a snapshot.
         * @return diagnostic string containing the event and container representations
         */
        public String toString() {
            return String.format("EventAndContext:{\"event\": %s, \"context\": %s}", event.toString(), context.toString());
        }

    }

}
