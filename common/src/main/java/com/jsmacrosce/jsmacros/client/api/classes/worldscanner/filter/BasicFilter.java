package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IAdvancedFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.logical.AndFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.logical.NotFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.logical.OrFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.logical.XorFilter;

/**
 * Base class for filters that can be combined into new logical filters without modifying
 * the receiver. {@code and} and {@code or} short-circuit; {@code xor} evaluates both filters.
 * <pre>
 * const NotFilter = Java.type("com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.logical.NotFilter");
 * const StringCompareFilter = Java.type("com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.compare.StringCompareFilter");
 * const filter = new NotFilter(new StringCompareFilter("EQUALS", "air"))
 *   .and(new StringCompareFilter("STARTS_WITH", "stone"));
 * filter.apply("stone_bricks");
 * </pre>
 *
 * @param <T> the type of values evaluated by the filter
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public abstract class BasicFilter<T> implements IAdvancedFilter<T> {

    /**
     * Returns a new filter accepting a value only when both filters accept it. The second
     * filter is evaluated only if this filter accepts the value.
     *
     * @param filter the filter that also has to accept a value for it to be kept
     * @return a new filter accepting a value only when both this and {@code filter} do
     * @since 1.6.5
     */
    @Override
    public IAdvancedFilter<T> and(IFilter<T> filter) {
        return new AndFilter<>(this, filter);
    }

    /**
     * Returns a new filter accepting a value when either filter accepts it. The second
     * filter is evaluated only if this filter rejects the value.
     *
     * @param filter the filter that can accept a value instead of this one
     * @return a new filter accepting a value when this or {@code filter} does
     * @since 1.6.5
     */
    @Override
    public IAdvancedFilter<T> or(IFilter<T> filter) {
        return new OrFilter<>(this, filter);
    }

    /**
     * Returns a new filter accepting a value when exactly one filter accepts it. Both
     * filters are evaluated.
     *
     * @param filter the filter that must disagree with this one for a value to be kept
     * @return a new filter accepting a value when exactly one of the two does
     * @since 1.6.5
     */
    @Override
    public IAdvancedFilter<T> xor(IFilter<T> filter) {
        return new XorFilter<>(this, filter);
    }

    /**
     * Returns a new filter accepting a value exactly when this filter rejects it.
     *
     * @return a new filter accepting a value exactly when this one rejects it
     * @since 1.6.5
     */
    @Override
    public IAdvancedFilter<T> not() {
        return new NotFilter<>(this);
    }

}
