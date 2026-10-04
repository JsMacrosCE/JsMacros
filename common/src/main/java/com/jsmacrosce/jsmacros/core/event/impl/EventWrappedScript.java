package com.jsmacrosce.jsmacros.core.event.impl;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.Event;

/**
 * The event a script is handed when it is being run as a wrapped script, which is the case when
 * something took it from
 * {@link com.jsmacrosce.jsmacros.core.library.impl.FJsMacros#wrapScriptRun(java.lang.String) wrapScriptRun()}
 * or {@code wrapScriptRunAsync} and then passed it on to a Java API that wants a callback,
 * filter, supplier or comparator. The arguments the caller passed come in as {@link #arg1} and
 * {@link #arg2}, and the value the script produces goes back out through one of the
 * {@code setReturn} methods, which is what the Java caller ends up with.<br>
 * This event is registered with the core event registry. The wrapper dispatches it before
 * starting the wrapped script, so {@code JsMacros.on("WrappedScript", ...)} can observe it.<br>
 * Which arguments are filled in depends on the functional interface the wrapper was passed as.
 * A single argument interface leaves {@code arg2} {@code null}, a no argument one leaves both
 * {@code null}, and a {@code Comparator} is given both of its values by the caller but only the
 * first one is forwarded here, so {@code arg2} is {@code null} for it too.<br>
 * Calling {@code cancel()} throws an {@code UnsupportedOperationException}, because this event
 * is not cancellable, and there is no game action behind it that could be stopped anyway.<br>
 * Note: the shipped type definitions do not carry the type parameters of this class through into
 * the event interface, so {@code arg1}, {@code arg2} and {@code result} are left untyped there.
 * example:
 * <pre>
 * // "sum.js", run through JsMacros.wrapScriptRun by the calling script. The `event`
 * // it receives is a WrappedScript: arg1 and arg2 are what the caller passed in,
 * // and setReturnInt is how the answer gets back to the caller.
 * event.setReturnInt(Number(event.arg1) + Number(event.arg2));
 * </pre>
 * @param <T> the type the first argument arrives as
 * @param <U> the type the second argument arrives as, and {@code null} unless the wrapper was
 *            passed as a two argument interface
 * @param <R> the type of the value the script returns to the caller
 * @author Wagyourtail
 * @since 1.7.0
 */
@DocletCategory("Core")
@Event("WrappedScript")
public class EventWrappedScript<T, U, R> extends BaseEvent {
    /**
     * the first argument the caller passed to the wrapped method, or {@code null} when the
     * wrapper was called through a no argument interface such as {@code Runnable} or
     * {@code Supplier}. The type is whatever the Java method the wrapper stands in for accepted,
     * so read it according to that method rather than expecting a fixed shape.
     */
    public final T arg1;
    /**
     * the second argument the caller passed to the wrapped method, or {@code null} for every
     * single argument interface and for a {@code Comparator}, which receives two values but only
     * has its first one forwarded here.
     */
    public final U arg2;

    /**
     * the value the script hands back to the Java caller. Assign to it directly or use one of the
     * {@code setReturn} methods, which do nothing more than box a value and store it here.<br>
     * Which of the wrapped interfaces actually read it back depends on what the wrapper was
     * passed as: {@code Function}, {@code BiFunction} and {@code Supplier} return it as it is,
     * {@code Predicate} and {@code BiPredicate} cast it to a {@code Boolean}, and
     * {@code Comparator} casts it to an {@code Integer}. {@code Consumer}, {@code BiConsumer} and
     * {@code Runnable} never look at it, so setting it there has no effect. Those casts are
     * unchecked, so handing back the wrong kind of value fails in the calling Java code rather
     * than in the script.<br>
     * The caller reads it once the script has finished, so set it while the script is still
     * running rather than from something the script kicks off and returns from.
     */
    public R result;

    public EventWrappedScript(Core<?, ?> runner, T arg1, U arg2) {
        super(runner);
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    public String toString() {
        return String.format("%s:{\"arg1\": %s, \"arg2\": %s}", this.getEventName(), arg1, arg2);
    }

    /**
     * hands a boolean back to the Java caller, which is the shape a {@code Predicate} or
     * {@code BiPredicate} reads out of {@link #result}. Use it when the wrapper was passed as one
     * of those, since the caller casts the result to a {@code Boolean} and anything else fails
     * there. Calling it more than once just overwrites the previous value.
     *
     * @param b the value to return to the caller
     */
    public void setReturnBoolean(boolean b) {
        result = (R) (Object) b;
    }

    /**
     * hands an integer back to the Java caller, which is the shape a {@code Comparator} reads out
     * of {@link #result}, and the shape a {@code Function} or {@code Supplier} that declares an
     * {@code int} return would get. A {@code Comparator} casts the result to an {@code Integer},
     * so handing back something else fails in the caller's sort.
     *
     * @param i the value to return to the caller
     */
    public void setReturnInt(int i) {
        result = (R) (Object) i;
    }

    /**
     * hands a double back to the Java caller, which suits a {@code Function} or {@code Supplier}
     * that returns a floating point number. There is no separate float setter;
     * {@code setReturnDouble} boxes the value as a {@code Double} and the wrapper hands it back
     * as it is, so a Java caller that needs a {@code float} or {@code Float} result has to use
     * {@link #setReturnObject(java.lang.Object) setReturnObject} with a {@code Float} instead.
     *
     * @param d the value to return to the caller
     */
    public void setReturnDouble(double d) {
        result = (R) (Object) d;
    }

    /**
     * hands a string back to the Java caller, which suits a {@code Function} or {@code Supplier}
     * that returns text.
     *
     * @param s the value to return to the caller
     */
    public void setReturnString(String s) {
        result = (R) (Object) s;
    }

    /**
     * hands an arbitrary value back to the Java caller, which is the general form for a
     * {@code Function} or {@code Supplier} that returns an object of the wrapper's own type. For
     * the narrower shapes prefer the specific setter, since the caller may be casting to a
     * particular type.
     *
     * @param o the value to return to the caller
     */
    public void setReturnObject(Object o) {
        result = (R) (Object) o;
    }

}
