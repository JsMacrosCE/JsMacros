package com.jsmacrosce.jsmacros.core.event;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceParams;

/** Determines whether an event name is supported and whether an event instance passes a filter. */
@DocletCategory("Events and Event Handling")
public interface EventFilterer {

    /** Reports whether this filterer supports the given event name.
     * @param event the event name
     * @return {@code true} if this filterer supports the name
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    @DocletReplaceParams("event: keyof Events")
    boolean canFilter(String event);

    /** Tests whether this event instance passes the filter.
     * @param event the event instance to test
     * @return {@code true} if the event passes
     */
    boolean test(BaseEvent event);

    /**
     * Extension hook for composed filterers to check references to themselves. The default
     * implementation rejects only a direct self-reference; implementers can also check contained
     * compound filterers. Obtain a composed filterer through {@link
     * com.jsmacrosce.jsmacros.core.library.impl.FJsMacros#createComposedEventFilterer(EventFilterer)}.
     * <pre>
     * const filterer = JsMacros.createModulusEventFilterer(20);
     * const composed = JsMacros.createComposedEventFilterer(filterer);
     * </pre>
     */
    @DocletCategory("Events and Event Handling")
    interface Compound extends EventFilterer {

        /** Checks whether {@code base} is this filterer; throws {@link IllegalArgumentException} if so.
         * @param base the compound filterer checked against this filterer
         */
        default void checkCyclicRef(Compound base) {
            if (this == base) throw new IllegalArgumentException("Cyclic reference detected.");
        }

    }

}
