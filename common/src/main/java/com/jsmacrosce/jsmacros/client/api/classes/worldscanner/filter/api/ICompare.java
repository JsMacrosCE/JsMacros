package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api;

import com.jsmacrosce.doclet.DocletCategory;
/**
 * A two-argument comparison function, used by string filters.
 *
 * @param <T> the type of values compared
 * @since 1.6.5
 */
@FunctionalInterface
@DocletCategory("Filters/Predicates")
public interface ICompare<T> {

    /**
     * Compares a candidate with a target, returning whether the comparison holds.
     *
     * @param obj1 the candidate being tested
     * @param obj2 the comparison target
     * @return whether the candidate satisfies the comparison
     * @since 1.6.5
     */
    boolean compare(T obj1, T obj2);

}
