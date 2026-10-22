package com.jsmacrosce.jsmacros.core.event.impl;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * Payload for scripts run through {@link com.jsmacrosce.jsmacros.core.library.impl.FJsMacros#wrapScriptRun(java.lang.String) wrapScriptRun()}
 * or {@code wrapScriptRunAsync}. The wrapper dispatches this event before running the script.
 * Arguments depend on the Java functional interface: no-argument interfaces provide two nulls,
 * one-argument interfaces provide {@code arg1} and a null {@code arg2}, and two-argument
 * interfaces provide both, except {@code Comparator}, which forwards only its first value.
 * The wrapped script can set {@link #result} for interfaces that return a value; consumers and
 * runnables ignore it. This event is not cancellable.
 * <pre>
 * // In a wrapped two-argument function script, return a result to the Java caller.
 * event.setReturnInt(Number(event.arg1) + Number(event.arg2));
 * </pre>
 * @param <T> the type the first argument arrives as
 * @param <U> the type the second argument arrives as, and {@code null} unless the wrapper was
 *            passed as a two argument interface
 * @param <R> the type of the value the script returns to the caller
 * @since 1.7.0
 */
@DocletCategory("Core")
@Event("WrappedScript")
public class EventWrappedScript<T, U, R> extends BaseEvent {
    /**
     * the first argument supplied by the wrapped interface, or {@code null} for no-argument
     * interfaces
     */
    public final T arg1;
    /**
     * the second argument supplied by a two-argument interface, or {@code null} otherwise
     */
    public final U arg2;

    /**
     * Value supplied by the script for the wrapped Java interface. Value-returning interfaces
     * read it after the script finishes: functions and suppliers return it as-is, predicates cast
     * it to {@code Boolean}, and comparators cast it to {@code Integer}. Consumers and runnables
     * ignore it; an incompatible predicate or comparator value fails in the Java caller.
     */
    public R result;

    /**
     * Stores the event's associated core and the supplied arguments without dispatching the event
     * or starting a script.
     *
     * @param runner the core associated with this event
     * @param arg1 the supplied first argument
     * @param arg2 the supplied second argument
     */
    public EventWrappedScript(Core<?, ?> runner, T arg1, U arg2) {
        super(runner);
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    /**
     * Returns diagnostic text containing the event name and formatted {@code arg1} and {@code arg2}
     * values. This is not a stable serialization or valid quoted JSON, and does not include
     * {@code result}.
     *
     * @return event-name and argument diagnostic text
     */
    public String toString() {
        return String.format("%s:{\"arg1\": %s, \"arg2\": %s}", this.getEventName(), arg1, arg2);
    }

    /**
     * Sets a boolean result, as read by {@code Predicate} and {@code BiPredicate} wrappers. Other
     * result types may fail their caller-side cast.
     *
     * @param b the value to return to the caller
     */
    public void setReturnBoolean(boolean b) {
        result = (R) (Object) b;
    }

    /**
     * Sets an integer result, as read by {@code Comparator} wrappers. Function and supplier
     * wrappers return the boxed value as-is; an incompatible comparator result fails its caller-side cast.
     *
     * @param i the value to return to the caller
     */
    public void setReturnInt(int i) {
        result = (R) (Object) i;
    }

    /**
     * Sets a double result, boxed as {@code Double} when returned by a function or supplier. To
     * return a {@code Float}, use {@link #setReturnObject(java.lang.Object)} with a {@code Float}.
     *
     * @param d the value to return to the caller
     */
    public void setReturnDouble(double d) {
        result = (R) (Object) d;
    }

    /**
     * Sets a string result for a function or supplier wrapper.
     *
     * @param s the value to return to the caller
     */
    public void setReturnString(String s) {
        result = (R) (Object) s;
    }

    /**
     * Sets an arbitrary result value. The wrapped interface may cast it to a particular type.
     *
     * @param o the value to return to the caller
     */
    public void setReturnObject(Object o) {
        result = (R) (Object) o;
    }

}
