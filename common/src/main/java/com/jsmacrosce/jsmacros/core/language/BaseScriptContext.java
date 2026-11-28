package com.jsmacrosce.jsmacros.core.language;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.IEventListener;
import com.jsmacrosce.jsmacros.core.service.ServiceManager;
import com.jsmacrosce.jsmacros.core.threads.JsMacrosThreadPool;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/** Holds a language runtime context and tracks threads and event containers associated with its execution.
 * Closing releases bound event locks, interrupts the threads the script owns, and removes this context
 * from its runner. Engine threads that only call into the script for a callback are excluded from that
 * interrupt wave, because interrupting a game or network thread mid-work corrupts state outside the script.
 * @param <T> language-specific runtime context type
 * @since 1.4.0
 */
public abstract class BaseScriptContext<T> {
    /** The {@code Core} that owns this context; the reference is fixed, but the runner itself is not immutable. */
    public final Core<?, ?> runner;
    protected volatile boolean preventLog = false;
    protected volatile boolean closed = false;
    private volatile boolean scriptStopRequested = false;
    /** Construction timestamp in milliseconds since the Unix epoch, not the language-execution start time. */
    public final long startTime = System.currentTimeMillis();

    private Object syncObjectPrivate = new Object();
    /** Weak reference to this context's synchronization object; clearing its private strong reference permits collection. */
    public final WeakReference<Object> syncObject = new WeakReference<>(this.syncObjectPrivate);

    /** Event supplied when this context was triggered, or {@code null} when none was supplied. */
    public final BaseEvent triggeringEvent;
    protected final File mainFile;

    /** The language-specific runtime context, or {@code null} before it is set. */
    protected T context = null;
    protected Thread mainThread = null;

    protected final Set<Thread> threads = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Threads bound only so a callback can re-enter this context. Engine threads, such as the
     * Minecraft render thread running a screen widget callback, must not be interrupted when the
     * context closes: interrupting the render thread mid-frame corrupts GL state, and interrupting
     * a network thread mid-packet breaks the connection. These entries are excluded from the
     * interrupt wave in {@link #closeContext()}.
     */
    protected final Set<Thread> borrowedThreads = Collections.newSetFromMap(new ConcurrentHashMap<>());

    protected final Map<Thread, EventContainer<? extends BaseScriptContext<T>>> events = new ConcurrentHashMap<>();

    // <listener, event>
    /** Live listener-to-event-name map used by event registration; its listener keys are weakly held. */
    public final WeakHashMap<IEventListener, String> eventListeners = new WeakHashMap<>();

    /** Whether a context-associated {@code MethodWrapper} has been constructed; this starts {@code false} and is used by {@code shouldKeepAlive}. */
    public boolean hasMethodWrapperBeenInvoked = false;

    /** Initializes a context with its runner and optional triggering event and script file.
     * @param runner owning runner
     * @param event triggering event, if any
     * @param file script file, if any
     */
    public BaseScriptContext(Core<?, ?> runner, BaseEvent event, File file) {
        this.runner = runner;
        this.triggeringEvent = event;
        this.mainFile = file;
    }

    /** Returns the sync object while it remains strongly referenced elsewhere, or {@code null} after collection.
     * @return sync object, or {@code null} if collected
     */
    public Object getSyncObject() {
        return syncObject.get();
    }

    /** Drops the context's strong reference to its sync object. */
    public void clearSyncObject() {
        this.syncObjectPrivate = null;
    }

    /** Returns whether the method-wrapper flag is set or a service requires this context to remain alive.
     * @return {@code true} if either condition holds
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean shouldKeepAlive() {
        return this.hasMethodWrapperBeenInvoked || ServiceManager.hasKeepAlive(this);
    }

    /** Returns the mutable map of threads to their bound event containers.
     * @return the backing map
     * @since 1.6.0
     */
    public synchronized Map<Thread, EventContainer<? extends BaseScriptContext<T>>> getBoundEvents() {
        return events;
    }

    /** Associates an event container with a thread, replacing any existing association.
     * @param th thread to associate
     * @param event event container to associate
     * @since 1.6.0
     */
    public synchronized void bindEvent(Thread th, EventContainer<BaseScriptContext<T>> event) {
        events.put(th, event);
    }

    /** Releases the event lock for the thread's bound event container, if present.
     * @param thread thread whose event lock is released
     * @return {@code true} if an event container was present; otherwise {@code false}
     * @since 1.6.0
     */
    public synchronized boolean releaseBoundEventIfPresent(Thread thread) {
        EventContainer<? extends BaseScriptContext<T>> event = events.get(thread);
        if (event != null) {
            event.releaseLock();
            return true;
        }
        return false;
    }

    /** Returns the language-specific runtime context.
     * @return runtime context, or {@code null} if unset
     */
    public T getContext() {
        return context;
    }

    /** Returns the main execution thread, or {@code null} before it is assigned.
     * @return the main thread, or {@code null} before assignment
     * @since 1.5.0
     */
    public Thread getMainThread() {
        return mainThread;
    }

    /** Adds a thread to this context's bound-thread set as a thread the script owns. Owned threads
     * are interrupted when the context closes.
     * @param t thread to bind; {@code null} and binding after closure are rejected
     * @return {@code true} if the thread was not already bound
     * @since 1.6.0
     */
    public synchronized boolean bindThread(Thread t) {
        if (closed) {
            throw new ScriptContextClosedException("Cannot bind thread to closed context");
        }
        if (t == null) {
            throw new ScriptAssertionError("Cannot bind null thread");
        }
        return threads.add(t);
    }

    /**
     * Binds the calling thread so it can dispatch a script callback.
     * <p>
     * A thread that only calls into the script for a single callback — for example the Minecraft
     * render thread running a screen button handler — does not belong to the script. It is
     * recorded as borrowed and left out of {@link #closeContext()}'s interrupt wave, because
     * interrupting it mid-frame or mid-packet corrupts state outside the script. Thread-pool
     * workers are still bound as owned, since a callback running on one can be interrupted
     * safely.
     *
     * @return {@code true} if the thread was not already bound
     * @since 2.2.0
     */
    @ApiStatus.Internal
    @DocletIgnore
    public synchronized boolean bindCallerThread() {
        if (closed) {
            throw new ScriptContextClosedException("Cannot bind thread to closed context");
        }
        Thread t = Thread.currentThread();
        boolean newlyBound = threads.add(t);
        // Mark only threads this call newly bound. A non-pool thread that a language explicitly
        // bound as owned keeps its interruptible status when it later dispatches a callback.
        if (newlyBound && !(t instanceof JsMacrosThreadPool.PoolThread)) {
            borrowedThreads.add(t);
        }
        return newlyBound;
    }

    /** Removes a bound thread and releases its associated event lock, if present.
     * @param t bound thread; throws {@link ScriptAssertionError} if it is not bound
     * @since 1.6.0
     */
    public synchronized void unbindThread(Thread t) {
        if (!threads.remove(t)) throw new ScriptAssertionError("Cannot unbind thread that is not bound");
        borrowedThreads.remove(t);
        EventContainer<?> container = events.get(t);
        if (container != null) {
            container.releaseLock();
        }
    }

    /** Returns the mutable backing set of threads bound to this context.
     * @return the live set of bound threads
     * @since 1.6.0
     */
    public synchronized Set<Thread> getBoundThreads() {
        return threads;
    }

    /** Assigns and binds the main thread; a second assignment is rejected.
     * @param t thread to assign
     * @since 1.5.0
     */
    public void setMainThread(Thread t) {
        if (this.mainThread != null) {
            throw new ScriptAssertionError("Cannot change main thread of context container once assigned!");
        }
        this.mainThread = t;
        bindThread(t);
    }

    /** Returns the event that triggered this script, or {@code null} if none was supplied.
     * @return triggering event, or {@code null}
     * @since 1.5.0
     */
    public BaseEvent getTriggeringEvent() {
        return triggeringEvent;
    }

    /** Sets the language runtime context once; a second assignment is rejected.
     * @param context runtime context to store
     */
    public void setContext(T context) {
        if (this.context != null) {
            throw new ScriptAssertionError("Context already set");
        }
        this.context = context;
    }

    /** Returns whether the context is closed; also closes it if its weakly held sync object was cleared.
     * Closing is done outside this context's monitor so a language runtime close can wait for
     * threads that need to unbind themselves.
     * @return {@code true} if closed
     */
    public boolean isContextClosed() {
        boolean needsClose;
        synchronized (this) {
            needsClose = !closed && syncObject.get() == null;
        }
        if (needsClose) {
            closeContext();
        }
        return closed;
    }

    /** Starts intentional shutdown once, before listener cleanup and context closure. */
    @ApiStatus.Internal
    @DocletIgnore
    public synchronized boolean beginScriptStop() {
        if (closed || scriptStopRequested) return false;
        scriptStopRequested = true;
        return true;
    }

    /** Returns whether this run requested intentional shutdown. */
    @ApiStatus.Internal
    @DocletIgnore
    public boolean isScriptStopRequested() {
        return scriptStopRequested;
    }

    /** Registers a listener atomically with respect to intentional shutdown. */
    @ApiStatus.Internal
    @DocletIgnore
    public synchronized void registerScriptListener(String event, IEventListener listener) {
        if (closed || scriptStopRequested) {
            throw new ScriptContextClosedException("Cannot register a listener in a stopping context");
        }
        runner.eventRegistry.addListener(event, listener);
        eventListeners.put(listener, event);
    }

    /** Removes listeners registered by this run, including interrupted event waits. */
    @ApiStatus.Internal
    @DocletIgnore
    public void removeScriptListeners() {
        ImmutableList<IEventListener> listeners;
        synchronized (this) {
            listeners = ImmutableList.copyOf(eventListeners.keySet());
            eventListeners.clear();
        }
        listeners.forEach(IEventListener::off);
    }

    /** Identifies cancellation caused by this run's explicit stop without hiding other failures. */
    @ApiStatus.Internal
    @DocletIgnore
    public boolean isScriptStopException(Throwable failure) {
        if (!scriptStopRequested) return false;
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof InterruptedException || cause instanceof ScriptContextClosedException || isLanguageCancellation(cause)) {
                return true;
            }
            if (cause.getCause() == cause) break;
        }
        return false;
    }

    /** Lets a language recognize its own runtime's cancellation exception. */
    protected boolean isLanguageCancellation(Throwable failure) {
        return false;
    }

    /** Closes this context and sets whether the execution wrapper suppresses its cancellation log.
     * A call made after the context already closed still records the flag but performs no teardown.
     * @param preventLog whether to suppress the cancellation log
     */
    public final void closeContext(boolean preventLog) {
        this.preventLog = preventLog;
        closeContext();
    }

    /**
     * Closes this context once: releases bound event locks, interrupts the threads the script
     * owns, gives the language a chance to close its runtime, and unregisters the context from
     * the runner. Later calls do nothing.
     * <p>
     * Threads bound only for callback dispatch through {@link #bindCallerThread()} are not
     * interrupted here; a language runtime may still apply its own cancellation while
     * {@link #doSubclassClose()} runs. The context monitor is released before locks are released,
     * threads are interrupted, or the language runtime closes: an interrupted thread may need to
     * unbind itself, and closing a language runtime may wait for those threads.
     */
    public final void closeContext() {
        final ImmutableList<EventContainer<? extends BaseScriptContext<T>>> boundEvents;
        final ImmutableSet<Thread> threadsToInterrupt;

        synchronized (this) {
            if (closed) return;
            closed = true;
            // fix concurrency issue the "fun" way
            boundEvents = ImmutableList.copyOf(getBoundEvents().values());
            threadsToInterrupt = Sets.difference(getBoundThreads(), borrowedThreads).immutableCopy();
        }

        boundEvents.forEach(EventContainer::releaseLock);
        threadsToInterrupt.forEach(Thread::interrupt);
        try {
            doSubclassClose();
        } finally {
            // Removal must happen even if the language runtime fails to close, or the runner would
            // keep a dead context in its open-context list.
            runner.getContexts().remove(this);
        }
    }

    /**
     * Closes language-specific runtime resources after base cleanup has released locks and
     * interrupted owned threads. Called at most once, outside the context monitor, so an
     * implementation may block while threads unwind from the interrupt.
     */
    protected void doSubclassClose() {
    }

    /** Returns the script file supplied when this context was created, if any.
     * @return the script file, or {@code null} if none was supplied
     * @since 1.6.0
     */
    @Nullable
    public File getFile() {
        return mainFile;
    }

    /** Returns the absolute containing folder for the script file, or the absolute configured macro folder if no file was supplied.
     * @return the absolute folder; throws {@link NullPointerException} if the script file has no parent
     * @since 1.6.0
     */
    public File getContainedFolder() {
        return mainFile == null ? runner.config.macroFolder.getAbsoluteFile() : mainFile.getParentFile().getAbsoluteFile();
    }

    /** Returns whether this language context supports concurrent execution on multiple threads.
     * @return {@code true} if concurrent execution on multiple threads is supported
     */
    public abstract boolean isMultiThreaded();

    /** Runs an operation that may block; the default implementation runs it directly.
     * @param sleep operation to run
     * @throws InterruptedException if the operation is interrupted
     */
    public void wrapSleep(SleepRunnable sleep) throws InterruptedException {
        sleep.run();
    }

    /**
     * Assertion error used when a script attempts an invalid context operation.
     * <pre>
     * const Failure = Java.type("com.jsmacrosce.jsmacros.core.language.BaseScriptContext$ScriptAssertionError");
     * const error = new Failure("invalid context operation");
     * Chat.log(error.getMessage());
     * </pre>
     */
    public static class ScriptAssertionError extends AssertionError {
        /** Creates an assertion error for an invalid script-context operation.
         * @param message failure description
         */
        public ScriptAssertionError(String message) {
            super(message);
        }

    }

    /** Distinguishes an attempt to use a closed context from unrelated script assertions. */
    @ApiStatus.Internal
    @DocletIgnore
    public static class ScriptContextClosedException extends ScriptAssertionError {
        public ScriptContextClosedException(String message) {
            super(message);
        }
    }

    /**
     * Operation accepted by {@code BaseScriptContext.wrapSleep(SleepRunnable)}; its
     * {@code run()} method may throw {@code InterruptedException}.
     * <pre>
     * const SleepRunnable = Java.type("com.jsmacrosce.jsmacros.core.language.BaseScriptContext$SleepRunnable");
     * const operation = new (Java.extend(SleepRunnable))({
     *     run() { Chat.log("operation"); }
     * });
     * operation.run();
     * </pre>
     */
    @FunctionalInterface
    public interface SleepRunnable {
        /** Runs the blocking operation.
         * @throws InterruptedException if interrupted
         */
        void run() throws InterruptedException;

    }

}
