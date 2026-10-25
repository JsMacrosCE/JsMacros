package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;

/**
 * Defines operations that combine a filter with another filter or negate it. Each operation
 * returns a new filter; {@code and} and {@code or} short-circuit, while {@code xor} evaluates
 * both filters.
 *
 * @param <T> the type of value tested by these filters
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
@DocletIgnore
public interface IAdvancedFilter<T> extends IFilter<T> {

    /**
     * Returns a filter accepting a value only if both filters accept it. The second filter is
     * evaluated only if this filter accepts the value.
     *
     * @param filter the other filter that must accept the value
     * @return a filter accepting a value only when both this filter and {@code filter} do
     * @since 1.6.5
     */
    IAdvancedFilter<T> and(IFilter<T> filter);

    /**
     * Returns a filter accepting a value if either filter accepts it. The second filter
     * is not evaluated when this filter accepts the value.
     *
     * @param filter the other filter to evaluate if this filter rejects the value
     * @return a filter accepting a value when this filter or {@code filter} does
     * @since 1.6.5
     */
    IAdvancedFilter<T> or(IFilter<T> filter);

    /**
     * Returns a filter accepting a value when exactly one filter accepts it. Both filters
     * are evaluated.
     *
     * @param filter the other filter
     * @return a filter accepting a value when exactly one of the filters accepts it
     * @since 1.6.5
     */
    IAdvancedFilter<T> xor(IFilter<T> filter);

    /**
     * Returns a filter accepting a value exactly when this filter rejects it.
     *
     * @return a filter accepting a value exactly when this filter rejects it
     * @since 1.6.5
     */
    IAdvancedFilter<T> not();

}
