package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.logical;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.BasicFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;

/**
 * A filter that accepts a value when its wrapped filter rejects it. It always evaluates the
 * wrapped filter; exceptions propagate and a {@code null} result causes a
 * {@link NullPointerException} when unboxed.
 * <p>
 * The scanner builder creates this filter for a {@code not} step, which negates the
 * category's accumulated filter.
 * <pre>
 * // Negation applies to the accumulated state filter.
 * const scanner = World.getWorldScanner()
 *   .withStateFilter("getLuminance").is("==", 15)
 *   .notStateFilter()
 *   .build();
 * const found = scanner.scanAroundPlayer(2);
 * Chat.log(`${found.size()} states that do not emit full light`);
 * </pre>
 *
 * @param <T> the type of values evaluated by the wrapped filter
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public class NotFilter<T> extends BasicFilter<T> {

    private final IFilter<T> filter;

    /**
     * Stores the filter whose result this filter negates.
     *
     * @param filter the filter to evaluate and negate
     * @since 1.6.5
     */
    public NotFilter(IFilter<T> filter) {
        this.filter = filter;
    }

    /**
     * Evaluates the wrapped filter and negates its result.
     *
     * @param obj the value to test
     * @return the negated result
     * @throws NullPointerException if the wrapped filter answers {@code null}, since the
     *         boxed result is unboxed to be negated
     * @since 1.6.5
     */
    @Override
    public Boolean apply(T obj) {
        return !filter.apply(obj);
    }

    /**
     * Returns the wrapped filter.
     *
     * @return the filter whose answer this one reverses
     * @since 1.6.5
     */
    public IFilter<T> getFilter() {
        return filter;
    }

}
