package com.jsmacrosce.jsmacros.core.library.impl.classes;

import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.EventLockWatchdog;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.config.CoreConfigV2;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.impl.EventWrappedScript;
import com.jsmacrosce.jsmacros.core.language.BaseScriptContext;
import com.jsmacrosce.jsmacros.core.language.EventContainer;

import java.util.function.Function;

/**
 * Wraps a script run as a {@link MethodWrapper} for Java callback interfaces. Calls that return a
 * value wait for the script even when the wrapper is asynchronous; calls that return nothing wait
 * only for synchronous wrappers. Synchronous calls start a lock watchdog using the configured
 * maximum lock time; if the lock remains held past that limit, the script context is closed.
 * The one-argument {@link #accept(Object)} method passes an existing {@link BaseEvent} through;
 * other forms create an {@link EventWrappedScript} payload.
 * <pre>
 * // Pass the wrapper to a Java API that accepts a callback.
 * const wrapper = JsMacros.wrapScriptRun("adder.js");
 * // Async wrappers skip waiting only for callbacks with no return value.
 * const later = JsMacros.wrapScriptRunAsync("adder.js");
 * </pre>
 * In the wrapped target script {@code adder.js}, a two-argument callback supplies
 * {@code arg1} and {@code arg2} through its {@link EventWrappedScript} payload:
 * <pre>
 * event.setReturnDouble(Number(event.arg1) + Number(event.arg2));
 * </pre>
 * @param <T> the type the first argument arrives as
 * @param <U> the type the second argument arrives as, and {@code null} unless the wrapper was
 *            passed as a two argument interface
 * @param <V> the type of the value the script returns to the caller
 */
public class WrappedScript<T, U, V> extends MethodWrapper<T, U, V, BaseScriptContext<?>> {
    private final Core<?, ?> runner;
    /**
     * Starts the run for the supplied event and returns its container, used for waiting and the
     * lock watchdog. Events created by the wrapper are dispatched before this function is called.
     */
    public final Function<BaseEvent, EventContainer<BaseScriptContext<?>>> f;
    /**
     * Whether this wrapper skips waiting for methods that return nothing. Value-returning methods
     * wait regardless of this flag.
     */
    public final boolean _async;

    /**
     * Stores the runner and callback used by this wrapper and configures whether void-returning
     * callbacks skip waiting.
     *
     * @param runner core used to create events and handle synchronous-call waiting
     * @param f function invoked with the event to start the run and return its container for waiting
     * @param _async whether void-returning callbacks skip waiting; value-returning callbacks wait
     *        regardless
     */
    public WrappedScript(Core<?, ?> runner, Function<BaseEvent, EventContainer<BaseScriptContext<?>>> f, boolean _async) {
        super();
        this.runner = runner;
        this.f = f;
        this._async = _async;
    }

    private EventContainer<BaseScriptContext<?>> execute(BaseEvent event) {
        if (event instanceof EventWrappedScript) event.trigger();
        return f.apply(event);
    }

    /**
     * Runs the script with one argument and discards its result. An existing {@link BaseEvent} is
     * passed through unchanged; any other value becomes {@code arg1} of an {@link EventWrappedScript}.
     * The call waits unless this wrapper is asynchronous.
     *
     * @param t the argument to pass to the script, and the value of {@code arg1} unless it is
     *          already an event
     */
    @Override
    public void accept(T t) {
        BaseEvent event = t instanceof BaseEvent ? (BaseEvent) t : new EventWrappedScript<>(runner, t, null);
        EventContainer<BaseScriptContext<?>> t1 = execute(event);
        if (!_async) {
            boolean joinedMain = runner.profile.checkJoinedThreadStack();
            if (joinedMain) {
                runner.profile.joinedThreadStack.add(t1.getLockThread());
            }
            EventLockWatchdog.startWatchdog(t1, null, runner.config.getOptions(CoreConfigV2.class).maxLockTime);
            try {
                t1.awaitLock(() -> runner.profile.joinedThreadStack.remove(t1.getLockThread()));
            } catch (InterruptedException ignored) {
                runner.profile.joinedThreadStack.remove(t1.getLockThread());
            }
        }
    }

    /**
     * Runs the script with {@code t} as {@code arg1} and {@code u} as {@code arg2}, discarding its
     * result. Unlike {@link #accept(java.lang.Object) accept}, this always creates an
     * {@link EventWrappedScript}. The call waits unless this wrapper is asynchronous.
     *
     * @param t the first argument, which the script reads as {@code arg1}
     * @param u the second argument, which the script reads as {@code arg2}
     */
    @Override
    public void accept(T t, U u) {
        EventContainer<BaseScriptContext<?>> t1 = execute(new EventWrappedScript<>(runner, t, u));
        if (!_async) {
            boolean joinedMain = runner.profile.checkJoinedThreadStack();
            if (joinedMain) {
                runner.profile.joinedThreadStack.add(t1.getLockThread());
            }
            EventLockWatchdog.startWatchdog(t1, null, runner.config.getOptions(CoreConfigV2.class).maxLockTime);
            try {
                t1.awaitLock(() -> runner.profile.joinedThreadStack.remove(t1.getLockThread()));
            } catch (InterruptedException ignored) {
                runner.profile.joinedThreadStack.remove(t1.getLockThread());
            }
        }
    }

    /**
     * Runs the script with {@code t} as {@code arg1} and {@code arg2} set to {@code null}, then
     * returns the event's result unchanged. Unlike {@link #accept(java.lang.Object) accept}, it
     * always creates an {@link EventWrappedScript}; it waits even when {@code _async} is set.
     *
     * @param t the argument to pass to the script, which the script reads as {@code arg1}
     * @return the event's result, or {@code null} if none was set
     */
    @Override
    public V apply(T t) {
        EventWrappedScript<T, U, V> e;
        EventContainer<BaseScriptContext<?>> t1 = execute(e = new EventWrappedScript<>(runner, t, null));
        boolean joinedMain = runner.profile.checkJoinedThreadStack();
        if (joinedMain) {
            runner.profile.joinedThreadStack.add(t1.getLockThread());
        }
        EventLockWatchdog.startWatchdog(t1, null, runner.config.getOptions(CoreConfigV2.class).maxLockTime);
        try {
            t1.awaitLock(() -> runner.profile.joinedThreadStack.remove(t1.getLockThread()));
        } catch (InterruptedException ignored) {
            runner.profile.joinedThreadStack.remove(t1.getLockThread());
        }
        return e.result;
    }

    /**
     * Runs the script with {@code t} as {@code arg1} and {@code u} as {@code arg2}, then returns
     * the event's result unchanged. It waits even when {@code _async} is set.
     *
     * @param t the first argument, which the script reads as {@code arg1}
     * @param u the second argument, which the script reads as {@code arg2}
     * @return the event's result, or {@code null} if none was set
     */
    @Override
    public V apply(T t, U u) {
        EventWrappedScript<T, U, V> e;
        EventContainer<BaseScriptContext<?>> t1 = execute(e = new EventWrappedScript<>(runner, t, u));
        boolean joinedMain = runner.profile.checkJoinedThreadStack();
        if (joinedMain) {
            runner.profile.joinedThreadStack.add(t1.getLockThread());
        }
        EventLockWatchdog.startWatchdog(t1, null, runner.config.getOptions(CoreConfigV2.class).maxLockTime);
        try {
            t1.awaitLock(() -> runner.profile.joinedThreadStack.remove(t1.getLockThread()));
        } catch (InterruptedException ignored) {
            runner.profile.joinedThreadStack.remove(t1.getLockThread());
        }
        return e.result;
    }

    /**
     * Runs the script with {@code t} as {@code arg1} and {@code arg2} set to {@code null}, then
     * casts the result to {@code Boolean}. It waits even when {@code _async} is set; a non-Boolean
     * result, including {@code null}, causes a {@link ClassCastException}.
     *
     * @param t the argument to pass to the script, which the script reads as {@code arg1}
     * @return the script's result cast to a boolean
     * @throws ClassCastException if the script's result is not a {@code Boolean}
     */
    @Override
    public boolean test(T t) {
        EventWrappedScript<T, U, V> e;
        EventContainer<BaseScriptContext<?>> t1 = execute(e = new EventWrappedScript<>(runner, t, null));
        boolean joinedMain = runner.profile.checkJoinedThreadStack();
        if (joinedMain) {
            runner.profile.joinedThreadStack.add(t1.getLockThread());
        }
        EventLockWatchdog.startWatchdog(t1, null, runner.config.getOptions(CoreConfigV2.class).maxLockTime);
        try {
            t1.awaitLock(() -> runner.profile.joinedThreadStack.remove(t1.getLockThread()));
        } catch (InterruptedException ignored) {
            runner.profile.joinedThreadStack.remove(t1.getLockThread());
        }
        return (Boolean) e.result;
    }

    /**
     * Runs the script with {@code t} as {@code arg1} and {@code u} as {@code arg2}, then casts its
     * result to {@code Boolean}. The call waits even when {@code _async} is set; a non-Boolean
     * result, including {@code null}, causes a {@link ClassCastException}.
     *
     * @param t the first argument, which the script reads as {@code arg1}
     * @param u the second argument, which the script reads as {@code arg2}
     * @return the script's result cast to a boolean, so {@code null} fails here as well
     * @throws ClassCastException if the script's result is not a {@code Boolean}
     */
    @Override
    public boolean test(T t, U u) {
        EventWrappedScript<T, U, V> e;
        EventContainer<BaseScriptContext<?>> t1 = execute(e = new EventWrappedScript<>(runner, t, u));
        boolean joinedMain = runner.profile.checkJoinedThreadStack();
        if (joinedMain) {
            runner.profile.joinedThreadStack.add(t1.getLockThread());
        }
        EventLockWatchdog.startWatchdog(t1, null, runner.config.getOptions(CoreConfigV2.class).maxLockTime);
        try {
            t1.awaitLock(() -> runner.profile.joinedThreadStack.remove(t1.getLockThread()));
        } catch (InterruptedException ignored) {
            runner.profile.joinedThreadStack.remove(t1.getLockThread());
        }
        return (Boolean) e.result;
    }

    /**
     * Runs the script with both {@code arg1} and {@code arg2} set to {@code null}; its result is
     * discarded. The caller waits unless {@code _async} is set.
     */
    @Override
    public void run() {
        EventContainer<BaseScriptContext<?>> t1 = execute(new EventWrappedScript<>(runner, null, null));
        if (!_async) {
            boolean joinedMain = runner.profile.checkJoinedThreadStack();
            if (joinedMain) {
                runner.profile.joinedThreadStack.add(t1.getLockThread());
            }
            EventLockWatchdog.startWatchdog(t1, null, runner.config.getOptions(CoreConfigV2.class).maxLockTime);
            try {
                t1.awaitLock(() -> runner.profile.joinedThreadStack.remove(t1.getLockThread()));
            } catch (InterruptedException ignored) {
                runner.profile.joinedThreadStack.remove(t1.getLockThread());
            }
        }
    }

    /**
     * Runs the script with {@code o1} as {@code arg1} and {@code arg2} set to {@code null}; the
     * comparator's second value, {@code o2}, is not passed to the script. The result is cast to
     * {@code Integer}, and the call waits even when {@code _async} is set. A non-Integer result,
     * including {@code null}, causes a {@link ClassCastException}.
     *
     * @param o1 the first of the two values, which the script reads as {@code arg1}
     * @param o2 the second of the two values, which is not passed on to the script
     * @return the script's result cast to an int, so {@code null} fails here as well
     * @throws ClassCastException if the script's result is not an {@code Integer}
     */
    @Override
    public int compare(T o1, T o2) {
        EventWrappedScript<T, U, V> e;
        EventContainer<BaseScriptContext<?>> t1 = execute(e = new EventWrappedScript<>(runner, o1, null));
        boolean joinedMain = runner.profile.checkJoinedThreadStack();
        if (joinedMain) {
            runner.profile.joinedThreadStack.add(t1.getLockThread());
        }
        EventLockWatchdog.startWatchdog(t1, null, runner.config.getOptions(CoreConfigV2.class).maxLockTime);
        try {
            t1.awaitLock(() -> runner.profile.joinedThreadStack.remove(t1.getLockThread()));
        } catch (InterruptedException ignored) {
            runner.profile.joinedThreadStack.remove(t1.getLockThread());
        }
        return (Integer) e.result;
    }

    /**
     * Runs the script with both {@code arg1} and {@code arg2} set to {@code null}, then returns its
     * result unchanged. The call waits even when {@code _async} is set.
     *
     * @return whatever the script put on the event's result, which is {@code null} unless the
     * script set it
     */
    @Override
    public V get() {
        EventWrappedScript<T, U, V> e;
        EventContainer<BaseScriptContext<?>> t1 = execute(e = new EventWrappedScript<>(runner, null, null));
        boolean joinedMain = runner.profile.checkJoinedThreadStack();
        if (joinedMain) {
            runner.profile.joinedThreadStack.add(t1.getLockThread());
        }
        EventLockWatchdog.startWatchdog(t1, null, runner.config.getOptions(CoreConfigV2.class).maxLockTime);
        try {
            t1.awaitLock(() -> runner.profile.joinedThreadStack.remove(t1.getLockThread()));
        } catch (InterruptedException ignored) {
            runner.profile.joinedThreadStack.remove(t1.getLockThread());
        }
        return e.result;
    }

}
