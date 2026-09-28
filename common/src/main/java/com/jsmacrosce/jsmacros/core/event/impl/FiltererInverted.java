package com.jsmacrosce.jsmacros.core.event.impl;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.EventFilterer;

/**
 * A filterer that lets through exactly the events its base filterer rejects. There is no public
 * constructor, this is built by
 * {@link com.jsmacrosce.jsmacros.core.library.impl.FJsMacros#invertEventFilterer(com.jsmacrosce.jsmacros.core.event.EventFilterer) invertEventFilterer()},
 * which is also the only way a script gets one.<br>
 * Inverting twice cancels out. Handing an already inverted filterer back to the factory returns
 * the filterer underneath it rather than a second wrapper, so a chain of inverts never grows and
 * {@code invert(invert(f))} really is {@code f}.<br>
 * Only the accept and reject decision flips, not which events the filterer can be used for: that
 * still comes from the base, so an inverted filterer is accepted by the {@code on} overload of
 * {@code JsMacros} for exactly the events its base was accepted for.<br>
 * Inverting a composed filterer flips the whole expression rather than one branch of it, so
 * inverting one built with {@code a.or(b)} rejects everything that either {@code a} or {@code b}
 * would have accepted, while inverting one built with {@code a.and(b)} rejects only what both of
 * them accepted.
 * example:
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
     * the filterer being inverted. This is the filterer that was handed to the factory, and for
     * a double invert it is the original one, since the factory unwraps the pair instead of
     * nesting a second inversion inside it.
     */
    public final EventFilterer base;

    /**
     * builds a filterer that accepts the events {@code base} rejects. If {@code base} is already
     * an inverted filterer its own base is returned instead, so inverting twice gives the
     * original filterer back rather than a doubled inversion.
     *
     * @param base the filterer to invert, which must not be {@code null}
     * @return the inverted filterer, or {@code base}'s own base if {@code base} was already
     * inverted
     * @throws IllegalArgumentException if {@code base} is {@code null}
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
     * whether this filterer can be used for the given event name. It is not a decision of its
     * own, the question is passed straight to the base filterer, so an inverted filterer can be
     * used for exactly the events its base can filter.
     *
     * @param event the name of the event being listened to
     * @return whatever the base filterer answers for {@code event}
     */
    @Override
    public boolean canFilter(String event) {
        return base.canFilter(event);
    }

    /**
     * the predicate this filterer evaluates, which is the base filterer's answer with the result
     * flipped. It only decides whether the listener runs, it never changes or blocks the event
     * itself.
     *
     * @param event the event being filtered
     * @return {@code true} if the base filterer rejects this event
     */
    @Override
    public boolean test(BaseEvent event) {
        return !base.test(event);
    }

    /**
     * walks this filterer looking for a reference cycle, so that a composed filterer that ends up
     * containing itself is caught while it is being built rather than when it is used. It checks
     * this filterer itself and then, if the base is a composed filterer, everything nested inside
     * that base as well.
     *
     * @param base the composed filterer the caller is building, which must not be reachable from
     *             this filterer
     * @throws IllegalArgumentException if this filterer, or any composed filterer nested inside
     *                                  its base, is {@code base} itself
     */
    @Override
    public void checkCyclicRef(Compound base) {
        Compound.super.checkCyclicRef(base);
        if (this.base instanceof Compound c) c.checkCyclicRef(base);
    }

}
