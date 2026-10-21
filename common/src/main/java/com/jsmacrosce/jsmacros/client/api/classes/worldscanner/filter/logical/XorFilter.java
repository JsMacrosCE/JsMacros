package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.logical;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.BasicFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;

/**
 * A filter that accepts a value only when exactly one of its two filters accepts it.
 * Both filters are evaluated, in order; if either returns {@code null}, evaluation throws
 * {@link NullPointerException}.
 *
 * @param <T> the type of value tested by the filters
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public class XorFilter<T> extends BasicFilter<T> {

    private final IFilter<T> filterOne;
    private final IFilter<T> filterTwo;

    /**
     * Combines the two filters using exclusive or; the first is evaluated before the second.
     *
     * @param filterOne the filter asked first
     * @param filterTwo the filter asked second
     * @since 1.6.5
     */
    public XorFilter(IFilter<T> filterOne, IFilter<T> filterTwo) {
        this.filterOne = filterOne;
        this.filterTwo = filterTwo;
    }

    /**
     * Tests a value against both filters, evaluating the first before the second.
     *
     * @param obj the value to test
     * @return whether exactly one of the two filters accepted it
     * @since 1.6.5
     */
    @Override
    public Boolean apply(T obj) {
        return filterOne.apply(obj) ^ filterTwo.apply(obj);
    }

    /**
     * Returns the first filter, evaluated before the second.
     *
     * @return the first of the two filters
     * @since 1.6.5
     */
    public IFilter<T> getFilterOne() {
        return filterOne;
    }

    /**
     * Returns the second filter, evaluated after the first.
     *
     * @return the second of the two filters
     * @since 1.6.5
     */
    public IFilter<T> getFilterTwo() {
        return filterTwo;
    }

}
