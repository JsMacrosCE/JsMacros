package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.logical;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.BasicFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;

/**
 * A filter that accepts a value only when both component filters accept it. The first filter
 * is evaluated before the second, which is skipped if the first rejects the value. Exceptions
 * from evaluated filters propagate; a {@code null} result causes a {@link NullPointerException}
 * when unboxed.
 * <p>
 * The scanner builder creates this filter for an {@code and} step, using the accumulated
 * filter as the first component; {@link BasicFilter#and(IFilter)} creates the same
 * combination from filters you already hold. Both components must test the same value type:
 * two numeric comparisons combine into a numeric predicate, while two
 * {@code BlockFilter} filters combine into a block predicate the scanner accepts.
 * The builder's {@code andBlockFilter} step does the latter for you.
 * <pre>
 * // The accumulated filter is tested first; the second is tested only if it accepts.
 * const scanner = World.getWorldScanner()
 *   .withBlockFilter("getHardness").is(">=", 10)
 *   .andBlockFilter("getSlipperiness").is(">", 0.5)
 *   .build();
 * const found = scanner.scanAroundPlayer(2);
 * Chat.log(`${found.size()} blocks that are both hard and slippery`);
 * </pre>
 * @param <T> the type of values evaluated by both filters
 *
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
@DocletIgnore
public class AndFilter<T> extends BasicFilter<T> {

    private final IFilter<T> filterOne;
    private final IFilter<T> filterTwo;

    /**
     * Combines two filters in the given evaluation order. The second is evaluated only if
     * the first accepts the value.
     *
     * @param filterOne the filter asked first
     * @param filterTwo the filter asked only when {@code filterOne} accepted
     * @since 1.6.5
     */
    public AndFilter(IFilter<T> filterOne, IFilter<T> filterTwo) {
        this.filterOne = filterOne;
        this.filterTwo = filterTwo;
    }

    /**
     * Tests a value using short-circuit conjunction: the second filter is evaluated only
     * when the first accepts it. Exceptions from evaluated filters propagate.
     *
     * @param obj the value to test
     * @return whether both filters accepted it
     * @throws NullPointerException if the first filter returns {@code null}, or if the second
     *         returns {@code null} after the first accepts the value
     * @since 1.6.5
     */
    @Override
    public Boolean apply(T obj) {
        return filterOne.apply(obj) && filterTwo.apply(obj);
    }

    /**
     * Returns the first filter.
     *
     * @return the first of the two filters
     * @since 1.6.5
     */
    public IFilter<T> getFilterOne() {
        return filterOne;
    }

    /**
     * Returns the second filter.
     *
     * @return the second of the two filters
     * @since 1.6.5
     */
    public IFilter<T> getFilterTwo() {
        return filterTwo;
    }

}
