package com.jsmacrosce.jsmacros.core.library;

import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.doclet.DocletReplaceParams;
import com.jsmacrosce.jsmacros.core.MethodWrapper;

/** Wraps script functions as Java-compatible {@link MethodWrapper} callbacks and exposes script-context controls.
 *
 * @since 1.2.5, re-named from {@code consumer} in 1.3.2
 */
@DocletIgnore
public interface IFWrapper<T> {

    /**
     * Wraps a script function for Java calls that wait for a result; execution may be scheduled on the script context.
     * @param c the script function
     * @return a Java-callable wrapper
     * @since 1.4.0
     */
    @DocletReplaceParams("c: (arg0: A, arg1: B) => R | void")
    <A, B, R> MethodWrapper<A, B, R, ?> methodToJava(T c);

    /**
     * Wraps a script function for Java calls that schedule the callback without returning its result to the Java caller.
     * @param c the script function
     * @return a Java-callable wrapper
     * @since 1.4.0
     */
    @DocletReplaceParams("c: (arg0: A, arg1: B) => R | void")
    <A, B, R> MethodWrapper<A, B, R, ?> methodToJavaAsync(T c);

    /**
     * Asynchronously wraps a function, requesting a queue priority where supported. The default implementation ignores the priority.
     *
     * @param priority requested queue priority; implementations may ignore it
     * @param c the script function
     * @param <A> type of the first callback argument
     * @param <B> type of the second callback argument
     * @param <R> callback result type
     * @return a Java-callable wrapper
     * @since 1.8.0
     */
    @DocletReplaceParams("priority: int, c: (arg0: A, arg1: B) => R | void")
    default <A, B, R> MethodWrapper<A, B, R, ?> methodToJavaAsync(int priority, T c) {
        return methodToJavaAsync(c);
    }

    /**
     * Defers the current task until queued tasks run, where supported. The default implementation throws {@link AssertionError}.
     * @since 1.8.0
     */
    default void deferCurrentTask() throws InterruptedException {
        throw new AssertionError("deferCurrentTask() is not implemented for this language");
    }

    /**
     * Adjusts the current task's queue priority before deferring it, where supported. The default implementation throws {@link AssertionError}.
     *
     * @param priorityAdjust amount added to the current task's priority
     * @since 1.8.0
     */
    default void deferCurrentTask(int priorityAdjust) throws InterruptedException {
        throw new AssertionError("deferCurrentTask() is not implemented for this language");
    }

    /**
     * Returns the current task's priority where supported. The default implementation throws {@link AssertionError}.
     * @since 1.8.0
     */
    default int getCurrentPriority() throws InterruptedException {
        throw new AssertionError("getCurrentPriority() is not implemented for this language");
    }

    /**
     * Stops the current script context.
     *
     * @since 1.2.2
     */
    void stop();

}
