package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api;

import com.jsmacrosce.doclet.DocletCategory;

import java.util.function.Function;

/**
 * A predicate that tests a value for acceptance. It is also a {@link Function} from
 * {@code T} to {@link Boolean}.
 *
 * @param <T> the type of value tested by this predicate
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public interface IFilter<T> extends Function<T, Boolean> {

    /**
     * Tests whether a value is accepted. Implementations used by parallel scans may be called
     * concurrently; callback filters supplied to the world scanner force sequential scanning.
     *
     * @param t the value to test
     * @return whether the value is accepted
     * @since 1.6.5
     */
    @Override
    Boolean apply(T t);

}
