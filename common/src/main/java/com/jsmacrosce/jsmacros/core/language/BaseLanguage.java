package com.jsmacrosce.jsmacros.core.language;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.config.ScriptTrigger;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.extensions.Extension;
import com.jsmacrosce.jsmacros.core.library.BaseLibrary;
import com.jsmacrosce.jsmacros.core.service.EventService;

import java.io.File;
import java.nio.file.FileSystemException;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Base implementation for script-language integrations. Triggered scripts run on the runner's thread pool and their contexts are closed after execution unless kept alive.
 *
 * @param <U> language-specific runtime context type parameter
 * @param <T> script context type used by this language implementation
 * @since 1.1.3
 */
public abstract class BaseLanguage<U, T extends BaseScriptContext<U>> {
    protected final Core<?, ?> runner;
    /** The extension associated with this language implementation. */
    public final Extension extension;

    /** Hook run on the worker thread before each trigger's execution handling begins; defaults to an empty action. A {@code null} hook or one that throws prevents normal execution, error handling, and cleanup. */
    public Runnable preThread = () -> {
    };

    /** Stores the extension and runner references used by this language implementation.
     * @param extension extension associated with this language
     * @param runner core used for execution and library lookup
     */
    public BaseLanguage(Extension extension, Core<?, ?> runner) {
        this.runner = runner;
        this.extension = extension;
    }

    /** Schedules execution of a configured script trigger. The script file is resolved against the runner's macro folder when relative and checked for existence and regular-file status on the worker thread. The returned container is created before queuing and does not represent a script result; missing or directory paths disable the original trigger and are reported through {@code catcher} when supplied. Failures while copying the trigger or creating its context occur before queuing. The worker's {@code preThread} hook runs outside the execution cleanup and error handling, so a null hook or one that throws skips those paths.
     * @param macro trigger configuration; its settings are copied for execution, while its enabled flag is disabled if the resolved file is missing or a directory
     * @param event triggering event, or {@code null}
     * @param then optional callback run after normal execution, or {@code null}; callback failures are logged directly
     * @param catcher optional handler for execution or file-check failures; failures thrown by it are logged
     * @return container holding the created context and its trigger lock
     */
    public final EventContainer<T> trigger(ScriptTrigger macro, BaseEvent event, Runnable then,
                                           Consumer<Throwable> catcher) {

        final ScriptTrigger staticMacro = macro.copy();
        final Thread ct = Thread.currentThread();
        final File file;
        if (macro.scriptFile.isAbsolute()) {
            file = macro.scriptFile.toFile();
        } else {
            file = runner.config.macroFolder.toPath().resolve(macro.scriptFile).toFile();
        }
        EventContainer<T> ctx = new EventContainer<>(createContext(event, file));
        runner.threadPool.runTask(() -> {
            Thread t = Thread.currentThread();
            preThread.run();
            try {
                if (event == null) {
                    t.setName(String.format("RunScript:{\"creator\":\"%s\"}", ct.getName()));
                } else {
                    t.setName(String.format("Script:{\"trigger\":\"%s\", \"event\":\"%s\", \"file\":\"%s\"}", staticMacro.triggerType, staticMacro.event, staticMacro.scriptFile));
                }
                if (file.exists() && file.isFile()) {

                    runner.addContext(ctx);

                    exec(ctx, staticMacro, event);
                    try {
                        if (then != null) {
                            then.run();
                        }
                    } catch (Throwable e) {
                        runner.profile.logError(e);
                    }
                } else {
                    macro.enabled = false;
                    if (staticMacro.scriptFile == null) {
                        throw new RuntimeException("No script file was selected for trigger" + staticMacro.event + "!");
                    } else {
                        throw new FileSystemException("file \"" + file.getPath() + "\" does not exist or is a directory!");
                    }
                }
            } catch (Throwable e) {
                try {
                    if (catcher != null) {
                        catcher.accept(e);
                    } else {
                        throw e;
                    }
                } catch (Throwable f) {
                    runner.profile.logError(f);
                }
            } finally {
                ctx.getCtx().unbindThread(Thread.currentThread());

                if (event instanceof EventService) {
                    runner.services.markCrashed(((EventService) event).serviceName);
                }

                EventContainer<?> cc = ctx.getCtx().events.get(Thread.currentThread());
                if (cc != null) {
                    cc.releaseLock();
                }

                ctx.getCtx().clearSyncObject();
                if (!ctx.getCtx().shouldKeepAlive()) {
                    ctx.getCtx().closeContext();
                }
            }
        }, th -> {
            ctx.setLockThread(th);
            ctx.getCtx().setMainThread(th);
        });
        return ctx;
    }

    /** Schedules execution of source text using the supplied language identifier; {@code fakeFile} supplies origin metadata, and {@code BaseLanguage} itself does not read or check it (the {@link #createContext(BaseEvent, File)} implementation may). Unlike the file-trigger overload, failures from {@code then} are passed to {@code catcher}. The worker's {@code preThread} hook runs outside execution error handling and cleanup, so a null hook or one that throws skips those paths.
     * @param lang language identifier passed to the implementation's execution hook
     * @param script source text to execute
     * @param fakeFile origin file metadata, or {@code null}
     * @param event triggering event, or {@code null}
     * @param then optional callback run after normal execution
     * @param catcher optional handler for execution or completion-callback failures
     * @return container holding the created context and its trigger lock
     */
    public final EventContainer<T> trigger(String lang, String script, File fakeFile, BaseEvent event, Runnable then, Consumer<Throwable> catcher) {
        final Thread ct = Thread.currentThread();
        EventContainer<T> ctx = new EventContainer<>(createContext(event, fakeFile));
        runner.threadPool.runTask(() -> {
            Thread t = Thread.currentThread();
            preThread.run();
            try {
                t.setName(String.format("RunScript:{\"creator\":\"%s\", \"start\":\"%d\"}", ct.getName(), System.currentTimeMillis()));

                runner.addContext(ctx);

                exec(ctx, lang, script, event);

                if (then != null) {
                    then.run();
                }
            } catch (Throwable e) {
                try {
                    if (catcher != null) {
                        catcher.accept(e);
                    } else {
                        throw e;
                    }
                } catch (Throwable f) {
                    // Skip logging if this is a result of `BaseScriptContext.closeContext(true)`
                    if (!"Context execution was cancelled.".equals(f.getMessage()) || !ctx.getCtx().preventLog) {
                        runner.profile.logError(f);
                    }
                }
            } finally {
                ctx.getCtx().unbindThread(Thread.currentThread());

                EventContainer<?> cc = ctx.getCtx().events.get(Thread.currentThread());
                if (cc != null) {
                    cc.releaseLock();
                }

                ctx.getCtx().clearSyncObject();
                if (!ctx.getCtx().shouldKeepAlive()) {
                    ctx.getCtx().closeContext();
                }
            }
        }, th -> {
            ctx.setLockThread(th);
            ctx.getCtx().setMainThread(th);
        });
        return ctx;
    }

    /** Retrieves shared, per-language, and per-execution libraries applicable to this context. Shared and per-language instances are reused; per-execution instances are constructed for this call. A per-execution library constructor failure propagates as a {@link RuntimeException}.
     * @param context context used to construct and select per-execution libraries
     * @return map of applicable library names to instances
     */
    public Map<String, BaseLibrary> retrieveLibs(T context) {
        return runner.libraryRegistry.getLibraries(this, context);
    }

    /** Retrieves the shared and per-language library instances without constructing per-execution libraries.
     * @return map of library names to shared or per-language instances
     */
    public Map<String, BaseLibrary> retrieveOnceLibs() {
        return runner.libraryRegistry.getOnceLibraries(this);
    }

    /** Creates new per-execution library instances for this call. A library constructor failure propagates as a {@link RuntimeException}.
     * @param context context used to construct and select per-execution libraries
     * @return map of per-execution library names to instances
     */
    public Map<String, BaseLibrary> retrievePerExecLibs(T context) {
        return runner.libraryRegistry.getPerExecLibraries(this, context);
    }

    /**
     * Executes a trigger script.
     *
     * @since 1.6.0
     */
    protected abstract void exec(EventContainer<T> ctx, ScriptTrigger macro, BaseEvent event) throws Exception;

    /**
     * Executes a source string using the requested language identifier.
     *
     * @since 1.7.0
     */
    protected abstract void exec(EventContainer<T> ctx, String lang, String script, BaseEvent event) throws Exception;

    /** Creates the language-specific script context used by trigger execution. Implementations are expected to return a non-null context.
     * @param event triggering event, or {@code null}
     * @param file origin file, or {@code null} for source-string execution
     * @return non-null context for the script run
     */
    public abstract T createContext(BaseEvent event, File file);

}
