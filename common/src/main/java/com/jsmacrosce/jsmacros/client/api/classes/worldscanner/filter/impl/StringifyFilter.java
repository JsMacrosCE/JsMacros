package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.impl;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.BasicFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.ICompare;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.compare.StringCompareFilter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Converts each candidate to its {@code toString()} text and accepts it if any configured pattern
 * matches. Patterns form a set, so duplicates have no effect; {@code MATCHES} uses a whole-string
 * regular-expression match.
 *
 * @param <T> the type of candidates converted to text
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public class StringifyFilter<T> extends BasicFilter<T> {

    private final Set<String> filterObjects;
    private final ICompare<String> filter;

    /**
     * Selects the comparison and starts with no patterns; until patterns are added, the filter
     * rejects every candidate.
     *
     * @param operation the comparison to run, spelled exactly as its constant:
     *                  {@code EQUALS}, {@code CONTAINS}, {@code STARTS_WITH},
     *                  {@code ENDS_WITH} or {@code MATCHES}
     * @throws IllegalArgumentException if {@code operation} is not one of those five spellings
     * @throws NullPointerException if {@code operation} is {@code null}
     * @since 1.6.5
     */
    public StringifyFilter(String operation) {
        this.filterObjects = new HashSet<>();
        filter = StringCompareFilter.FilterMethod.valueOf(operation).getMethod();
    }

    /**
     * Adds one pattern to the set and returns this filter; duplicate patterns have no effect.
     * @param toAdd the pattern to add
     * @return this filter, for chaining
     * @since 1.6.5
     */
    public StringifyFilter addOption(String toAdd) {
        filterObjects.add(toAdd);
        return this;
    }

    /**
     * Adds several patterns to the set and returns this filter.
     * @param toAdd the patterns to add
     * @return this filter, for chaining
     * @throws NullPointerException if {@code toAdd} is {@code null} or contains {@code null}
     * @since 1.6.5
     */
    public StringifyFilter addOption(String... toAdd) {
        filterObjects.addAll(List.of(toAdd));
        return this;
    }

    /**
     * Removes one pattern from the set and returns this filter.
     * @param toRemove the pattern to remove
     * @return this filter, for chaining
     * @since 1.6.5
     */
    public StringifyFilter removeOption(String toRemove) {
        filterObjects.remove(toRemove);
        return this;
    }

    /**
     * Removes several patterns from the set and returns this filter.
     * @param toRemove the patterns to remove
     * @return this filter, for chaining
     * @throws NullPointerException if {@code toRemove} is {@code null} or contains
     *         {@code null}
     * @since 1.6.5
     */
    public StringifyFilter removeOption(String... toRemove) {
        List.of(toRemove).forEach(filterObjects::remove);
        return this;
    }

    /**
     * Tests the candidate's {@code toString()} text against the patterns, accepting it when any
     * pattern matches. With no patterns, it rejects every candidate.
     *
     * @param obj the candidate to turn into text and test
     * @return whether any of the patterns matched the text
     * @throws NullPointerException if {@code obj} is {@code null}
     * @since 1.6.5
     */
    @Override
    public Boolean apply(Object obj) {
        String toCompare = obj.toString();
        return filterObjects.parallelStream().anyMatch(filterElement -> filter.compare(toCompare, filterElement));
    }

}
