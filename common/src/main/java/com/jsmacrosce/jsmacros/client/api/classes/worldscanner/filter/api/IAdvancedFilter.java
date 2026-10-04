package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api;

import com.jsmacrosce.doclet.DocletCategory;

/**
 * a filter that can be combined with another one.
 * <p>
 * Every combination method takes a second filter of the same element type and returns a
 * <i>new</i> filter, never a modified one: {@code and}, {@code or} and {@code xor} each wrap
 * the receiver and the argument together, and {@code not} wraps the receiver alone. Nothing
 * in the receiver is touched, so a filter that has already been used in a chain can be reused
 * as the starting point of a second one without the two interfering. That is what lets the
 * scanner builder keep one filter per category and replace it wholesale.
 * <p>
 * The return type is this same interface rather than a narrower one, so a chain of
 * combinations is itself combinable: the result of an {@code and} can be negated or combined
 * again. The wrapping is nested, so a long chain evaluates from the inside out, and the
 * result is a tree of two filter tests rather than a list.
 * <p>
 * The three binary combinations differ in one way that shows up in a scan. The
 * {@code and} and {@code or} filters short circuit, so the second filter is not asked at all
 * when the first has already settled the answer, while the xor filter evaluates both because
 * an exclusive or is not expressible as a short circuit in Java. A filter that is expensive or
 * that would throw on some inputs is therefore reached less often under {@code and} and
 * {@code or} than under {@code xor}.
 * <br>
 * <b>This interface is not in the shipped TypeScript definitions and a script never names
 * it.</b> The only script-facing route is {@code World.getWorldScanner()}, whose builder
 * creates these filters internally: an {@code and} or {@code or} step on the builder calls
 * the matching method here, and a {@code not} step calls {@code not}. The builder has no step
 * that produces an exclusive or, so although this interface offers one, the xor filter is
 * never built from a script.
 * example:
 * <pre>
 * // andStateFilter and orStateFilter are the builder calling and and or on
 * // the filter it already holds. Each step wraps what came before rather
 * // than editing it, so the order of the steps is the order of the wrapping
 * const scanner = World.getWorldScanner()
 *   .withBlockFilter("getHardness").is(">=", 10)
 *   .orBlockFilter("getSlipperiness").is("==", 1.0)
 *   .build();
 * const found = scanner.scanAroundPlayer(2);
 * Chat.log(`${found.size()} hard or slippery blocks`);
 *
 * // notStateFilter negates the whole state filter built so far, so it has to
 * // come after a state filter exists. A not with nothing to negate throws
 * const unlit = World.getWorldScanner()
 *   .withStateFilter("getLuminance").is("==", 15)
 *   .notStateFilter()
 *   .build();
 * </pre>
 *
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public interface IAdvancedFilter<T> extends IFilter<T> {

    /**
     * combines this filter and another so that a value is kept only when both accept it.
     * <p>
     * The result is a new filter and this one is left untouched. The second filter is not
     * asked when this one has already rejected the value, because the combination is
     * evaluated with a short circuiting and on the unboxed results.
     * <p>
     * A script reaches this through the {@code and} steps on the scanner builder, such as
     * {@code andBlockFilter} and {@code andStateFilter}, which hand the builder's existing
     * filter in as the receiver.
     * example:
     * <pre>
     * // andBlockFilter is the builder wrapping the block filter it already
     * // holds together with the one named here
     * const scanner = World.getWorldScanner()
     *   .withBlockFilter("getHardness").is(">=", 10)
     *   .andBlockFilter("getSlipperiness").is(">", 0.5)
     *   .build();
     * const found = scanner.scanAroundPlayer(2);
     * Chat.log(`${found.size()} hard and slippery blocks`);
     * </pre>
     *
     * @param filter the filter that also has to accept a value for it to be kept
     * @return a new filter accepting a value only when both this and {@code filter} do
     * @since 1.6.5
     */
    IAdvancedFilter<T> and(IFilter<T> filter);

    /**
     * combines this filter and another so that a value is kept when either one accepts it.
     * <p>
     * The result is a new filter and this one is left untouched. The second filter is not
     * asked when this one has already accepted the value, because the combination is
     * evaluated with a short circuiting or on the unboxed results.
     * <p>
     * A script reaches this through the {@code or} steps on the scanner builder, such as
     * {@code orBlockFilter} and {@code orStateFilter}, which hand the builder's existing
     * filter in as the receiver.
     * example:
     * <pre>
     * // orStateFilter is the builder wrapping the state filter it already
     * // holds together with the one named here, and the two are alternatives
     * const scanner = World.getWorldScanner()
     *   .withStateFilter("isAir").is(true)
     *   .orStateFilter("isLiquid").is(true)
     *   .build();
     * const found = scanner.scanAroundPlayer(2);
     * Chat.log(`${found.size()} air or liquid states`);
     * </pre>
     *
     * @param filter the filter that can accept a value instead of this one
     * @return a new filter accepting a value when this or {@code filter} does
     * @since 1.6.5
     */
    IAdvancedFilter<T> or(IFilter<T> filter);

    /**
     * combines this filter and another so that a value is kept only when exactly one of the
     * two accepts it.
     * <p>
     * The result is a new filter and this one is left untouched. Unlike the {@code and} and
     * {@code or} combinations, both filters are always asked, since an exclusive or cannot
     * be evaluated with a short circuit.
     * <p>
     * <b>No script can reach this.</b> The scanner builder offers {@code and}, {@code or} and
     * {@code not} steps but has no step for an exclusive or, so nothing a script writes calls
     * this method, and the xor filter it would produce is never built.
     * example:
     * <pre>
     * // there is no builder step that reaches an exclusive or, so this shows
     * // the shape of the filter rather than a chain a script can write.
     * // Both filters are always asked, even when the first one has already
     * // decided the answer, which is what separates xor from or.
     * // A Java caller would write: new XorFilter(first, second)
     * Chat.log("an exclusive or would accept a value exactly one filter wants");
     * </pre>
     *
     * @param filter the filter that must disagree with this one for a value to be kept
     * @return a new filter accepting a value when exactly one of the two does
     * @since 1.6.5
     */
    IAdvancedFilter<T> xor(IFilter<T> filter);

    /**
     * negates this filter, so a value is kept exactly when this one rejects it.
     * <p>
     * The result is a new filter wrapping this one and this one is left untouched, so
     * negating a filter that is already part of a chain does not disturb that chain.
     * <p>
     * A script reaches this through the {@code notStateFilter} and {@code notBlockFilter}
     * steps on the scanner builder. Those steps take no arguments and apply to the whole
     * filter of that category built so far rather than to the last step alone, and they
     * throw if nothing of that category has been configured yet.
     * example:
     * <pre>
     * // notStateFilter negates the entire state filter, not just the step
     * // before it, so this keeps everything that is not a light source
     * const scanner = World.getWorldScanner()
     *   .withStateFilter("getLuminance").is("==", 15)
     *   .notStateFilter()
     *   .build();
     * const found = scanner.scanAroundPlayer(2);
     * Chat.log(`${found.size()} states that do not emit light`);
     * </pre>
     *
     * @return a new filter accepting a value exactly when this one rejects it
     * @since 1.6.5
     */
    IAdvancedFilter<T> not();

}
