package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.logical;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.BasicFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;

/**
 * a filter that keeps a value exactly when the one it wraps rejects it.
 * <p>
 * It holds a single filter, fixed when it is built, and answers the opposite of what that
 * filter answers. Unlike the and, or and xor filters there is nothing to combine it with: it
 * is a negation of one filter rather than a relation between two, and it is the only one of
 * the four built from a single filter rather than from a pair.
 * <p>
 * The wrapped result is a boxed value, unboxed to be negated, so a filter that answers
 * {@code null} rather than yes or no causes a failure here rather than being treated as a
 * rejection. A failure inside the wrapped filter is not caught either, so a method based
 * filter that prints a stack trace and answers {@code false} will have that answer negated —
 * a method that throws inside a negated filter keeps the value rather than dropping it.
 * <br>
 * <b>This class is not in the shipped TypeScript definitions and a script never names it.</b>
 * The scanner builder creates it when a script writes a {@code not} step. Those steps take no
 * arguments and apply to the whole filter of that category built so far rather than to the
 * last step alone, and they fail if nothing of that category has been configured yet — a
 * negation with nothing to negate has no filter to wrap.
 * example:
 * <pre>
 * // notStateFilter negates the whole state filter, so it has to follow a
 * // state filter that exists. This keeps every state that is not emitting
 * // full light, rather than only the last step's states
 * const scanner = World.getWorldScanner()
 *   .withStateFilter("getLuminance").is("==", 15)
 *   .notStateFilter()
 *   .build();
 * const found = scanner.scanAroundPlayer(2);
 * Chat.log(`${found.size()} states that do not emit full light`);
 *
 * // the same for the block category. A not with no block filter behind it
 * // has nothing to negate, so the not step refuses it there and then:
 * // notBlockFilter throws on the spot, and build is never reached
 * const softer = World.getWorldScanner()
 *   .withBlockFilter("getHardness").is(">=", 10)
 *   .notBlockFilter()
 *   .build();
 * Chat.log(`${softer.scanAroundPlayer(2).size()} blocks softer than 10`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public class NotFilter<T> extends BasicFilter<T> {

    private final IFilter<T> filter;

    /**
     * fixes the filter to negate.
     * <p>
     * The wrapped filter is not modified, so negating a filter that is already part of a
     * chain leaves that chain as it was.
     *
     * @param filter the filter whose answer is reversed
     * @since 1.6.5
     */
    public NotFilter(IFilter<T> filter) {
        this.filter = filter;
    }

    /**
     * tests a value and reverses what the wrapped filter answered.
     * <p>
     * The wrapped filter is always asked; negation happens to the answer rather than to the
     * test, so there is nothing to short circuit here even where the wrapped filter has
     * several parts of its own.
     * <p>
     * A script does not call this. It is what a {@code not} step on the scanner builder ends
     * up testing each state with.
     * example:
     * <pre>
     * // this is the test behind notStateFilter, applied to the whole state
     * // filter rather than to the last step alone. isAir answered false for
     * // every non air state, and reversing that answer keeps the air ones
     * const scanner = World.getWorldScanner()
     *   .withStateFilter("isAir").is(false)
     *   .notStateFilter()
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} air states`);
     * </pre>
     *
     * @param obj the value to test
     * @return the opposite of what the wrapped filter answered
     * @throws NullPointerException if the wrapped filter answers {@code null}, since the
     *         boxed result is unboxed to be negated
     * @since 1.6.5
     */
    @Override
    public Boolean apply(T obj) {
        return !filter.apply(obj);
    }

    /**
     * the filter being negated.
     * <p>
     * A script does not call this: the builder keeps its filter to itself and never hands it
     * back, so there is no step that reads it.
     * example:
     * <pre>
     * // there is no step that reads a filter back from the builder, so
     * // nothing a script writes reaches this
     * Chat.log("a builder never hands its filter back to a script");
     * </pre>
     *
     * @return the filter whose answer this one reverses
     * @since 1.6.5
     */
    public IFilter<T> getFilter() {
        return filter;
    }

}
