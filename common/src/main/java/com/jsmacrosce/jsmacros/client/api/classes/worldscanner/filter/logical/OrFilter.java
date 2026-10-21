package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.logical;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.BasicFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;

/**
 * Combines two filters with short-circuit OR: the second is tested only if the first rejects
 * the value. A {@code null} result causes a {@link NullPointerException} when unboxed, and
 * exceptions from evaluated filters propagate. The scanner builder uses the accumulated filter
 * as the first component of an {@code or} step.
 * <pre>
 * // The two block conditions are alternatives: either one can match.
 * const scanner = World.getWorldScanner()
 *   .withBlockFilter("getHardness").is(">=", 10)
 *   .orBlockFilter("getSlipperiness").is("==", 1.0)
 *   .build();
 * const found = scanner.scanAroundPlayer(2);
 * Chat.log(`${found.size()} blocks that are hard or fully slippery`);
 * </pre>
 *
 * @param <T> the type of values evaluated by the filters
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public class OrFilter<T> extends BasicFilter<T> {

    private final IFilter<T> filterOne;
    private final IFilter<T> filterTwo;

    /**
     * Combines two filters in evaluation order; the second is tested only if the first rejects.
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
     * Tests a value using short-circuit OR. The second filter is evaluated only if the first
     * rejects the value; exceptions from evaluated filters propagate.
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
     * Returns the filter evaluated first.
     *
     * @return the first of the two filters
     * @since 1.6.5
     */
    public IFilter<T> getFilterOne() {
        return filterOne;
    }

    /**
     * Returns the filter evaluated second, only if the first rejects.
     *
     * @return the second of the two filters
     * @since 1.6.5
     */
    public IFilter<T> getFilterTwo() {
        return filterTwo;
    }

}
