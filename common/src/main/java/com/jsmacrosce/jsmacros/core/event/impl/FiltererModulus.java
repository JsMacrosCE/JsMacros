package com.jsmacrosce.jsmacros.core.event.impl;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.EventFilterer;

/**
 * A filterer that lets through only every nth event, which is a cheap way to thin out a listener
 * on an event that fires far too often to handle one by one. Build one with
 * {@link com.jsmacrosce.jsmacros.core.library.impl.FJsMacros#createModulusEventFilterer(int) createModulusEventFilterer()}
 * and hand it to the {@code JsMacros.on} overload that takes a filterer.<br>
 * It decides by counting rather than by looking at the event. The nth call to its predicate
 * returns {@code true} and resets the count, and every other call returns {@code false} and keeps
 * counting, so a quotient of {@code 3} lets the 3rd, 6th and 9th event through. The count starts
 * at zero, so the first event that reaches a fresh filterer is normally rejected.<br>
 * The quotient is taken as an absolute value, so a negative one behaves like its positive
 * counterpart rather than letting nothing through, and a quotient of {@code 0} lets everything
 * through, because the count is always at least zero.<br>
 * It can be used with any event, since its predicate never looks at the event name. The count
 * lives on the filterer itself rather than on the listener, so two listeners sharing one filterer
 * share one counter and take turns rather than each getting their own every nth event.
 * example:
 * <pre>
 * // a tick listener that only does its work once every 20 ticks, about once a second
 * const filterer = JsMacros.createModulusEventFilterer(20);
 * const listener = JsMacros.on("Tick", filterer, JavaWrapper.methodToJava(function () {
 *   Chat.log("a second went by");
 * }));
 * // later
 * JsMacros.off(listener);
 * </pre>
 * @author aMelonRind
 * @since 1.9.1
 */
@DocletCategory("Event Filterers")
@SuppressWarnings("unused")
public class FiltererModulus implements EventFilterer {
    /**
     * how many events have to arrive before one is let through. The constructor and
     * {@link #setQuotient(int)} store it as an absolute value, so it is never negative through
     * those, and a value of {@code 0} lets everything through.<br>
     * The field is writable, but assigning to it directly skips the absolute value. A negative
     * number written straight to the field is stored as it is, and since the count is never
     * negative that lets every event through rather than none. Prefer
     * {@link #setQuotient(int)} over writing to it.
     */
    public int quotient;
    /**
     * how many events have been counted since the last one was let through. It is reset to zero
     * every time this filterer accepts an event, which is what keeps the every nth pattern going.
     * <br>
     * The field is writable, and writing to it shifts the phase of the counter, so a script that
     * changes it takes over the counting from wherever the filterer happens to be.
     */
    public int count = 0;

    public FiltererModulus(int quotient) {
        this.quotient = Math.abs(quotient);
    }

    /**
     * whether this filterer can be used for the given event name. It answers {@code true} for
     * every event, since it counts events rather than looking at what they are, so the {@code on}
     * overload of {@code JsMacros} that takes a filterer will accept it for any event.
     *
     * @param event the name of the event being listened to
     * @return always {@code true}
     */
    @Override
    public boolean canFilter(String event) {
        return true;
    }

    /**
     * the predicate this filterer evaluates. It counts the call and lets one through every time
     * the count reaches the {@code quotient}, resetting the count afterwards and returning
     * {@code true}; on every other call it keeps counting and returns {@code false}. The event
     * itself is never looked at, so nothing about the event can make it pass early.<br>
     * It only decides whether the listener runs, it never changes or blocks the event.
     *
     * @param event the event being filtered, which this filterer does not inspect
     * @return {@code true} once per {@code quotient} calls
     */
    @Override
    public boolean test(BaseEvent event) {
        if (++count >= quotient) {
            count = 0;
            return true;
        }
        return false;
    }

    /**
     * sets how many events have to arrive before one is let through. The value is stored as an
     * absolute value, so a negative quotient behaves like its positive counterpart rather than
     * letting nothing through, and {@code 0} lets everything through. The count is left as it is,
     * so changing the quotient takes effect from the next event on. Returns the same filterer so
     * calls can be chained.
     *
     * @param quotient the number of events to skip between accepted ones
     * @return this filterer, for chaining
     */
    public FiltererModulus setQuotient(int quotient) {
        this.quotient = Math.abs(quotient);
        return this;
    }

}
