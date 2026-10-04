package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.logical;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.BasicFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;

/**
 * a filter that keeps a value when either of two filters accepts it.
 * <p>
 * It holds exactly two filters, fixed when it is built, and never changes them afterwards.
 * That is what distinguishes it from a filter holding a list: there is no way to add a third
 * alternative or to take one away, and the only way to combine further is to wrap this filter
 * in another one, which nests rather than extends.
 * <p>
 * The two are asked in order and the second is not asked at all when the first has already
 * accepted the value, so the filters are tested left to right with a short circuit. The
 * results are the boxed values the two filters return, unboxed to be combined, which means a
 * filter that answers {@code null} rather than yes or no causes a failure here rather than
 * being treated as a rejection. Unlike the xor filter, the second is skipped when the first
 * has already settled the answer, so the two are not both asked every time.
 * <br>
 * <b>This class is not in the shipped TypeScript definitions and a script never names it.</b>
 * The scanner builder creates it when a script writes an {@code or} step, handing it the
 * filter it already holds as the first one.
 * example:
 * <pre>
 * // orBlockFilter is the builder wrapping the block filter it already
 * // holds together with the one named in the step. The result is this
 * // filter, and the two it holds are alternatives rather than requirements
 * const scanner = World.getWorldScanner()
 *   .withBlockFilter("getHardness").is(">=", 10)
 *   .orBlockFilter("getSlipperiness").is("==", 1.0)
 *   .build();
 * const found = scanner.scanAroundPlayer(2);
 * Chat.log(`${found.size()} blocks that are hard or fully slippery`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public class OrFilter<T> extends BasicFilter<T> {

    private final IFilter<T> filterOne;
    private final IFilter<T> filterTwo;

    /**
     * fixes the two filters to be combined as alternatives.
     * <p>
     * The order matters, because it is the order they are asked in and the second is skipped
     * when the first has already accepted a value.
     *
     * @param filterOne the filter asked first
     * @param filterTwo the filter asked only when {@code filterOne} rejected
     * @since 1.6.5
     */
    public OrFilter(IFilter<T> filterOne, IFilter<T> filterTwo) {
        this.filterOne = filterOne;
        this.filterTwo = filterTwo;
    }

    /**
     * tests a value against both filters, stopping if the first accepts it.
     * <p>
     * The second filter is not asked when the first has already said yes, so a filter that
     * is expensive or that would fail on some values is reached less often here than under a
     * filter that always asks both sides.
     * <p>
     * A script does not call this. It is what an {@code or} step on the scanner builder ends
     * up testing each state with.
     * example:
     * <pre>
     * // orStateFilter is the builder wrapping the state filter it already
     * // holds together with the one named in the step. A state that is air
     // // never reaches the second filter
     * const scanner = World.getWorldScanner()
     *   .withStateFilter("isAir").is(true)
     *   .orStateFilter("isLiquid").is(true)
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} states that are air or liquid`);
     * </pre>
     *
     * @param obj the value to test
     * @return whether either filter accepted it
     * @throws NullPointerException if either filter answers {@code null}, since the two
     *         boxed results are unboxed to be combined
     * @since 1.6.5
     */
    @Override
    public Boolean apply(T obj) {
        return filterOne.apply(obj) || filterTwo.apply(obj);
    }

    /**
     * the filter asked first.
     * <p>
     * This is the filter the combination was built around, which on a builder chain is the
     * one accumulated so far rather than the one named in the step that caused the
     * combination. A script does not call this: the builder keeps its filter to itself and
     * never hands it back.
     * example:
     * <pre>
     * // there is no step that reads a filter back, so nothing a script
     * // writes reaches this. The getter exists for a Java caller holding
     * // the filter directly
     * Chat.log("a builder never hands its filter back to a script");
     * </pre>
     *
     * @return the first of the two filters
     * @since 1.6.5
     */
    public IFilter<T> getFilterOne() {
        return filterOne;
    }

    /**
     * the filter asked second, and only when the first rejected.
     * <p>
     * This is the one named in the step that caused the combination. A script does not call
     * this: the builder keeps its filter to itself and never hands it back.
     * example:
     * <pre>
     * // as with the other one, there is no step that reads a filter back
     * // from the builder, so a script cannot inspect what it combined
     * Chat.log("a builder never hands its filter back to a script");
     * </pre>
     *
     * @return the second of the two filters
     * @since 1.6.5
     */
    public IFilter<T> getFilterTwo() {
        return filterTwo;
    }

}
