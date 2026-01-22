package com.jsmacrosce.jsmacros.core.event;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletReplaceParams;

/**
 * @author aMelonRind
 * @since 1.9.1
 */
@DocletCategory("Events and Event Handling")
public interface EventFilterer {

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    @DocletReplaceParams("event: keyof Events")
    boolean canFilter(String event);

    boolean test(BaseEvent event);

    @DocletCategory("Events and Event Handling")
    interface Compound extends EventFilterer {

        default void checkCyclicRef(Compound base) {
            if (this == base) throw new IllegalArgumentException("Cyclic reference detected.");
        }

    }

}
