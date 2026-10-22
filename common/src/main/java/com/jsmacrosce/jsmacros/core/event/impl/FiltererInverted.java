package com.jsmacrosce.jsmacros.core.event.impl;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.EventFilterer;

/**
 * Inverts an {@link EventFilterer}'s test result without changing which event names it can filter.
 * Create one with {@link com.jsmacrosce.jsmacros.core.library.impl.FJsMacros#invertEventFilterer(EventFilterer)};
 * inverting an inverted filterer returns its original base.
 * <pre>
 * // every block update that is not dirt
 * const filterer = JsMacros.invertEventFilterer(
 *   JsMacros.createEventFilterer("BlockUpdate").setBlockId("dirt")
 * );
 * const listener = JsMacros.on("BlockUpdate", filterer, JavaWrapper.methodToJava(function (event) {
 *   Chat.log(`${event.block.getId()} changed somewhere that is not dirt`);
 * }));
 * // later
 * JsMacros.off(listener);
 * </pre>
 * @author aMelonRind
 * @since 1.9.1
 */
@DocletCategory("Event Filterers")
public class FiltererInverted implements EventFilterer.Compound {
    /**
     * The filterer whose test result is negated. When an inverted filterer is inverted, this
     * field instead refers to that filterer's base.
     */
    public final EventFilterer base;

    /**
     * Returns a filterer that negates {@code base}'s test result. If {@code base} is already an
     * inverted filterer, returns its base instead. Throws {@link IllegalArgumentException} if
     * {@code base} is {@code null}.
     *
     * @param base the filterer to invert
     * @return the inverted filterer, or {@code base}'s base if it was already inverted
     */
    public static EventFilterer invert(EventFilterer base) {
        if (base == null) throw new IllegalArgumentException("base cannot be null!");
        if (base.getClass() == FiltererInverted.class) return ((FiltererInverted) base).base;
        return new FiltererInverted(base);
    }

    private FiltererInverted(EventFilterer base) {
        this.base = base;
    }

    /**
     * Reports whether the base filterer supports the event name.
     *
     * @param event the name of the event being listened to
     * @return the base filterer's answer for {@code event}
     */
    @Override
    public boolean canFilter(String event) {
        return base.canFilter(event);
    }

    /**
     * Tests the base predicate and negates its result.
     *
     * @param event the event being filtered
     * @return {@code true} if the base filterer rejects this event
     */
    @Override
    public boolean test(BaseEvent event) {
        return !base.test(event);
    }

    /**
     * Checks this filterer and any composed base recursively for a reference to the candidate
     * composition.
     *
     * @param base the composed filterer the caller is building, which must not be reachable from
     *             this filterer
     */
    @Override
    public void checkCyclicRef(Compound base) {
        Compound.super.checkCyclicRef(base);
        if (this.base instanceof Compound c) c.checkCyclicRef(base);
    }

}
