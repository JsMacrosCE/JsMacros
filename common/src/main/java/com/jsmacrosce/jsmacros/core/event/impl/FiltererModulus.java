package com.jsmacrosce.jsmacros.core.event.impl;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.EventFilterer;

/**
 * A stateful filterer that lets a listener through after every configured number of predicate
 * calls. Build one with
 * {@link com.jsmacrosce.jsmacros.core.library.impl.FJsMacros#createModulusEventFilterer(int) createModulusEventFilterer()}
 * and hand it to the {@code JsMacros.on} overload that takes a filterer.<br>
 * The predicate counts calls, not event data: with a positive quotient of {@code 3}, the third,
  * sixth, and ninth calls pass. A fresh filterer's first call is rejected when its quotient is
  * greater than {@code 1}. The counter belongs to the filterer, so listeners sharing it also
  * share its count. It can be used for any event.
 * The constructor and setter apply {@link Math#abs(int)}; {@code Integer.MIN_VALUE} remains
 * negative, and a non-positive quotient makes every call pass.
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
     * Threshold checked by the predicate. The constructor and {@link #setQuotient(int)} apply
     * {@link Math#abs(int)}, but direct assignment does not; non-positive values make every call
     * pass.
     */
    public int quotient;
    /**
     * Number of calls counted since the last accepted call. It resets to zero when a call passes;
     * direct assignment changes the current counting phase.
     */
    public int count = 0;

    /**
     * Initializes the threshold and starts the call counter at zero. The threshold is normalized
     * with {@link Math#abs(int)}, except that {@code Integer.MIN_VALUE} remains negative.
     *
     * @param quotient threshold in predicate calls; its absolute value is used, except for {@code Integer.MIN_VALUE}
     */
    public FiltererModulus(int quotient) {
        this.quotient = Math.abs(quotient);
    }

    /**
     * Accepts any event name; this filterer does not inspect the event type.
     *
     * @param event the name of the event being listened to
     * @return always {@code true}
     */
    @Override
    public boolean canFilter(String event) {
        return true;
    }

    /**
     * Counts the call and passes when the count reaches {@code quotient}, then resets the count.
     * It does not inspect or modify the event.
     *
     * @param event the event being filtered
     * @return whether the call passes the filter
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
     * Sets the threshold, applying {@link Math#abs(int)}. The current count is preserved, and the
     * new threshold is used on the next call. Zero and {@code Integer.MIN_VALUE} make every call
     * pass.
     *
     * @param quotient threshold for the number of predicate calls
     * @return this filterer, for chaining
     */
    public FiltererModulus setQuotient(int quotient) {
        this.quotient = Math.abs(quotient);
        return this;
    }

}
