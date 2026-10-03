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
 * the thing behind {@code JsMacros.wrapScriptRun} and {@code wrapScriptRunAsync}: instead of
 * holding a function, it holds a whole script, and calling any of the methods on it starts that
 * script in its own run with an {@link EventWrappedScript} as its payload.<br>
 * It exists because a script written as a file is not a Java object, so it cannot be handed to a
 * Java API that wants a callback, a filter, a supplier or a comparator. This wraps the run in
 * something that implements all of them at once, by way of
 * {@link com.jsmacrosce.jsmacros.core.MethodWrapper MethodWrapper}, so the same wrapper can be
 * passed wherever any one of those is expected. Which method the Java caller ends up calling is
 * what decides the shape of the run, and each of the nine does the same three things: build the
 * event, run the script through {@link #f}, and then either return the value the script produced
 * or throw it away.
 * <br>
 * It is not the same thing as {@code JavaWrapper.methodToJava}, which is the other way of getting
 * a wrapper and the one to reach for when the code to run is already in this script. That wraps
 * the function itself; this one wraps a separate file or a language and script string. Both hand
 * back a {@code MethodWrapper}, so the two are interchangeable from the Java side, and neither is
 * this class, which a script never sees.
 * <br>
 * Two behaviours are worth knowing. The first is that a run started through {@code wrapScriptRun}
 * blocks the Java caller until the run's lock is released, which is when that script run is over;
 * {@code wrapScriptRunAsync} leaves the wait off, but only for the three methods that return
 * nothing. The methods that hand a value back always wait even in the async form, since there is
 * nothing to return until the script has answered, so an async wrapper used as a function is
 * still a blocking one.<br>
 * The second is that the wait is guarded. An
 * {@link com.jsmacrosce.jsmacros.core.EventLockWatchdog EventLockWatchdog} is started against
 * every run at the default limit, so a wrapped script that joins a thread the caller is already on
 * and never comes back has its context taken down instead of hanging the caller for good. The
 * watchdog is started without a listener here, so the timeout is reported through the profile's
 * log rather than through any event. While the run holds its lock, the calling thread is also put
 * on the profile's joined thread stack, so code the wrapped script runs on it is treated as being
 * on the main thread.<br>
 * This class is absent from the shipped TypeScript definitions. Nothing reachable from a
 * whitelisted signature names it, because {@code wrapScriptRun} is declared as returning a
 * {@code MethodWrapper}, so the type parameters below are only visible in these docs.
 * example:
 * <pre class="language-typescript">
 * // the calling side. the wrapper is what gets passed to whatever Java API
 * // wants a callback, and this form blocks the caller until the script is done
 * const wrapper = JsMacros.wrapScriptRun("adder.js");
 *
 * // "adder.js" is resolved against the macro folder, not against the folder of
 * // the script that wrapped it
 *
 * // inside "adder.js": `event` is a WrappedScript event, and arg1 is what the
 * // Java caller passed. arg2 is only filled in when the wrapper was passed to
 * // the Java side as a two argument interface, so on the one argument forms
 * // this reads as null, and Number(null) is 0. the answer goes back out
 * // through one of the setReturn methods, which the wrapper hands on for the
 * // interface it was passed as. the cast is for the editor's benefit, since
 * // the shipped typings give the `event` global the type of any event rather
 * // than of this one
 * const wrapped = event as any;
 * wrapped.setReturnDouble(Number(wrapped.arg1) + Number(wrapped.arg2));
 *
 * // the async form is the same wrapper with the wait left off, so it is the
 * // one to use when the Java API is holding on to it and will call it later
 * const later = JsMacros.wrapScriptRunAsync("adder.js");
 * </pre>
 * @param <T> the type the first argument arrives as
 * @param <U> the type the second argument arrives as, and {@code null} unless the wrapper was
 *            passed as a two argument interface
 * @param <V> the type of the value the script returns to the caller
 */
public class WrappedScript<T, U, V> extends MethodWrapper<T, U, V, BaseScriptContext<?>> {
    private final Core<?, ?> runner;
    /**
     * the run itself, handed in by whoever made the wrapper. It takes the
     * {@link com.jsmacrosce.jsmacros.core.event.BaseEvent BaseEvent} that was built for the call
     * and gives back the container the script is running in, which is what the wait and the
     * watchdog are done against. For the wrappers {@code JsMacros.wrapScriptRun} and
     * {@code wrapScriptRunAsync} build it is {@code Core.exec} on the wrapped script's file or
     * source, with the event name taken from the event that was just built, so the run is
     * triggered as a {@code "WrappedScript"} event.
     */
    public final Function<BaseEvent, EventContainer<BaseScriptContext<?>>> f;
    /**
     * whether this wrapper leaves the wait off. It is set by the call that made the wrapper and is
     * never changed afterwards, and it is the difference between
     * {@code JsMacros.wrapScriptRun} and {@code wrapScriptRunAsync}. It is only consulted by the
     * three methods that return nothing; the ones that hand a value back wait either way.
     */
    public final boolean _async;

    public WrappedScript(Core<?, ?> runner, Function<BaseEvent, EventContainer<BaseScriptContext<?>>> f, boolean _async) {
        super();
        this.runner = runner;
        this.f = f;
        this._async = _async;
    }

    /**
     * starts the script with a single argument, and does nothing with what it produces. This is
     * the {@code Consumer} form.<br>
     * It is the one method that does not always wrap its argument: if what it was given is
     * already a {@link com.jsmacrosce.jsmacros.core.event.BaseEvent BaseEvent} that goes straight
     * to the script, so a wrapper passed as a {@code Consumer} of some event type hands the
     * script that event rather than an {@link EventWrappedScript} wrapping it. Anything else is
     * wrapped, and the script reads it as {@code arg1}.<br>
     * Whether the caller waits for the script to finish depends on {@code _async}, so a wrapper
     * from {@code wrapScriptRun} blocks the Java thread that called it and one from
     * {@code wrapScriptRunAsync} does not. What the script sets as its result is dropped, so
     * there is nothing to hand back here.
     *
     * @param t the argument to pass to the script, and the value of {@code arg1} unless it is
     *          already an event
     */
    @Override
    public void accept(T t) {
        BaseEvent event = t instanceof BaseEvent ? (BaseEvent) t : new EventWrappedScript<>(runner, t, null);
        EventContainer<BaseScriptContext<?>> t1 = f.apply(event);
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
     * starts the script with two arguments, and does nothing with what it produces. This is the
     * {@code BiConsumer} form, and unlike {@link #accept(java.lang.Object) accept} it always
     * wraps: the two values go in as {@code arg1} and {@code arg2}.<br>
     * Whether the caller waits depends on {@code _async}, the same as for the single argument
     * form, and the script's result is dropped.
     *
     * @param t the first argument, which the script reads as {@code arg1}
     * @param u the second argument, which the script reads as {@code arg2}
     */
    @Override
    public void accept(T t, U u) {
        EventContainer<BaseScriptContext<?>> t1 = f.apply(new EventWrappedScript<>(runner, t, u));
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
     * starts the script with a single argument and hands back what it produced. This is the
     * {@code Function} form.<br>
     * The argument always goes in as {@code arg1} and {@code arg2} is {@code null}, even if the
     * value is already an event, since the pass through on {@link #accept(java.lang.Object) accept}
     * is only for the form that returns nothing. The result is read straight off the event and
     * returned as it is, with no cast, so it is the script that has to produce something the
     * Java method the wrapper stands in for can take.<br>
     * This waits for the script even when {@code _async} is set, since there is nothing to return
     * before it has answered, so an async wrapper used as a function still blocks the caller.
     *
     * @param t the argument to pass to the script, which the script reads as {@code arg1}
     * @return whatever the script put on the event's result, which is {@code null} unless the
     * script set it
     */
    @Override
    public V apply(T t) {
        EventWrappedScript<T, U, V> e;
        EventContainer<BaseScriptContext<?>> t1 = f.apply(e = new EventWrappedScript<>(runner, t, null));
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
     * starts the script with two arguments and hands back what it produced. This is the
     * {@code BiFunction} form, and is the only one that puts a second value in front of the
     * script.<br>
     * The result is read straight off the event and returned as it is, and the call waits for the
     * script even when {@code _async} is set.
     *
     * @param t the first argument, which the script reads as {@code arg1}
     * @param u the second argument, which the script reads as {@code arg2}
     * @return whatever the script put on the event's result, which is {@code null} unless the
     * script set it
     */
    @Override
    public V apply(T t, U u) {
        EventWrappedScript<T, U, V> e;
        EventContainer<BaseScriptContext<?>> t1 = f.apply(e = new EventWrappedScript<>(runner, t, u));
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
     * starts the script with a single argument and casts what it produced to a
     * {@code Boolean}. This is the {@code Predicate} form, and the cast is unchecked and happens
     * here rather than in the script, so a script that sets anything else fails the Java caller
     * with a {@link ClassCastException} rather than raising anything in the script itself.<br>
     * {@code arg2} is {@code null}, and the call waits for the script even when {@code _async} is
     * set.
     *
     * @param t the argument to pass to the script, which the script reads as {@code arg1}
     * @return the script's result cast to a boolean, so {@code null} fails here as well
     * @throws ClassCastException if the script's result is not a {@code Boolean}
     */
    @Override
    public boolean test(T t) {
        EventWrappedScript<T, U, V> e;
        EventContainer<BaseScriptContext<?>> t1 = f.apply(e = new EventWrappedScript<>(runner, t, null));
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
     * starts the script with two arguments and casts what it produced to a {@code Boolean}. This
     * is the {@code BiPredicate} form, and casts the same unchecked way
     * {@link #test(java.lang.Object) test} does.<br>
     * The call waits for the script even when {@code _async} is set.
     *
     * @param t the first argument, which the script reads as {@code arg1}
     * @param u the second argument, which the script reads as {@code arg2}
     * @return the script's result cast to a boolean, so {@code null} fails here as well
     * @throws ClassCastException if the script's result is not a {@code Boolean}
     */
    @Override
    public boolean test(T t, U u) {
        EventWrappedScript<T, U, V> e;
        EventContainer<BaseScriptContext<?>> t1 = f.apply(e = new EventWrappedScript<>(runner, t, u));
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
     * starts the script with no arguments at all and does nothing with what it produces. This is
     * the {@code Runnable} form, so both {@code arg1} and {@code arg2} are {@code null} for the
     * script.<br>
     * Whether the caller waits depends on {@code _async}, the same as for
     * {@link #accept(java.lang.Object) accept}, so this is the one to use for a Java API that
     * wants something to call and does not need an answer.
     */
    @Override
    public void run() {
        EventContainer<BaseScriptContext<?>> t1 = f.apply(new EventWrappedScript<>(runner, null, null));
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
     * starts the script with one of the two values a sort is comparing and casts what it produced
     * to an {@code Integer}. This is the {@code Comparator} form.<br>
     * Only the first value is forwarded: the script gets {@code o1} as {@code arg1} and
     * {@code arg2} is {@code null}, so a wrapped script used as a comparator has no way to see
     * what it is being compared against. The cast to {@code Integer} is unchecked and happens
     * here, so a script that sets anything else makes the sort fail with a
     * {@link ClassCastException} rather than raising anything in the script itself.<br>
     * The call waits for the script even when {@code _async} is set.
     *
     * @param o1 the first of the two values, which the script reads as {@code arg1}
     * @param o2 the second of the two values, which is not passed on to the script
     * @return the script's result cast to an int, so {@code null} fails here as well
     * @throws ClassCastException if the script's result is not an {@code Integer}
     */
    @Override
    public int compare(T o1, T o2) {
        EventWrappedScript<T, U, V> e;
        EventContainer<BaseScriptContext<?>> t1 = f.apply(e = new EventWrappedScript<>(runner, o1, null));
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
     * starts the script with no arguments and hands back what it produced. This is the
     * {@code Supplier} form, so both {@code arg1} and {@code arg2} are {@code null} for the
     * script.<br>
     * The result is read straight off the event and returned as it is, and the call waits for the
     * script even when {@code _async} is set, so a supplier built from an async wrapper is still a
     * blocking one.
     *
     * @return whatever the script put on the event's result, which is {@code null} unless the
     * script set it
     */
    @Override
    public V get() {
        EventWrappedScript<T, U, V> e;
        EventContainer<BaseScriptContext<?>> t1 = f.apply(e = new EventWrappedScript<>(runner, null, null));
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
