package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.impl;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.BasicFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.ICompare;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.compare.StringCompareFilter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * a filter that turns each candidate into text and keeps it when the text satisfies one of
 * the given patterns.
 * <p>
 * This is what a {@code withStringBlockFilter} or {@code withStringStateFilter} step becomes.
 * Where a method filter reflects on a helper and compares a value, this one takes no method at
 * all: it asks the candidate for its own text and compares that. A helper's text is its
 * overridden string form, which is a wrapper around a block id, so a pattern written as a bare
 * block id will not match — the text is the whole wrapper, not the id in it. A step naming a
 * method that returns a string is the way to get at a value such as the id on its own.
 * <p>
 * The patterns are a set rather than a single value, and a candidate is kept when
 * <i>any</i> of them matches. That is why a step takes as many strings as are useful and
 * treats them as alternatives rather than as a conjunction, and why the option list can be
 * edited after the filter is built. The set holds no duplicates, so adding the same pattern
 * twice changes nothing.
 * <p>
 * Two details about the matching are worth knowing. The five patterns are the same five the
 * method based string filters use, and one of them — {@code matches} — is a whole string
 * match rather than a search, so a pattern has to allow for the text around what it is
 * looking for. And the patterns are checked on a parallel stream, which makes no difference
 * to the answer, since the result is only whether any one of them matched.
 * <br>
 * <b>This class is not in the shipped TypeScript definitions and a script never names it.</b>
 * The builder constructs it from a string filter step, and the patterns come straight from
 * the arguments written in the step.
 * example:
 * <pre>
 * // the block helper stringifies to BlockHelper:{"id": "minecraft:coal_ore"},
 * // so a pattern for a bare block id would not match it. matches is a whole
 * // string match, which is what the leading and trailing .* are for
 * const scanner = World.getWorldScanner()
 *   .withStringBlockFilter().matches(".*coal_ore.*")
 *   .build();
 * const found = scanner.scanAroundPlayer(2);
 * Chat.log(`${found.size()} coal ore blocks`);
 *
 * // several patterns are alternatives, not a conjunction, so this keeps a
 * // block whose text matches any one of the three
 * const any = World.getWorldScanner()
 *   .withStringBlockFilter().matches(".*_ore.*", ".*_block.*", ".*dirt.*")
 *   .build();
 * Chat.log(`${any.scanAroundPlayer(2).size()} blocks of any of those three kinds`);
 *
 * // the state helper stringifies to a wrapper around the id and the
 * // properties, so a property can be matched the same way. The exact text
 * // is the property map rendered by its own toString, so a pattern like
 * // this one is written against what that rendering gives in practice
 * // rather than being fixed by the filter
 * const facing = World.getWorldScanner()
 *   .withStringStateFilter().matches(".*facing=north.*")
 *   .build();
 * Chat.log(`${facing.scanAroundPlayer(2).size()} states facing north`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public class StringifyFilter<T> extends BasicFilter<T> {

    private final Set<String> filterObjects;
    private final ICompare<String> filter;

    /**
     * fixes the comparison to run, with the pattern list left empty.
     * <p>
     * The comparison name is resolved here rather than per test, and a name that is not one
     * of the five is refused here. A filter built this way keeps nothing, because there are
     * no patterns for it to match against; the patterns are added by whatever step follows,
     * and a step written with no arguments at all adds none, leaving the filter matching
     * nothing.
     *
     * @param operation the comparison to run, spelled exactly as its constant:
     *                  {@code EQUALS}, {@code CONTAINS}, {@code STARTS_WITH},
     *                  {@code ENDS_WITH} or {@code MATCHES}
     * @throws IllegalArgumentException if {@code operation} is not one of those five, which
     *         includes the lower case and mixed case spellings of them
     * @since 1.6.5
     */
    public StringifyFilter(String operation) {
        this.filterObjects = new HashSet<>();
        filter = StringCompareFilter.FilterMethod.valueOf(operation).getMethod();
    }

    /**
     * adds one pattern to the set.
     * <p>
     * The filter is modified in place and returned, so calls chain. The set holds no
     * duplicates, so adding a pattern that is already there changes nothing and adding the
     * same pattern twice does not make it count twice.
     *
     * @param toAdd the pattern to add
     * @return this filter, for chaining
     * @since 1.6.5
     */
    public StringifyFilter addOption(String toAdd) {
        filterObjects.add(toAdd);
        return this;
    }

    /**
     * adds several patterns to the set at once.
     * <p>
     * A pattern already in the set is not added again, so the result depends only on which
     * patterns are present rather than how many times each was supplied. That matters because
     * the patterns are alternatives: repeating one cannot make a candidate more likely to
     * match.
     *
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
     * removes one pattern from the set.
     * <p>
     * Removing a pattern that is not there changes nothing, and the filter is returned
     * unchanged in that case rather than failing.
     *
     * @param toRemove the pattern to remove
     * @return this filter, for chaining
     * @since 1.6.5
     */
    public StringifyFilter removeOption(String toRemove) {
        filterObjects.remove(toRemove);
        return this;
    }

    /**
     * removes several patterns from the set at once.
     * <p>
     * Each is removed at most once, so a pattern has to appear only once in the set for one
     * call to remove it, and removing a pattern that is not there changes nothing.
     *
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
     * turns the candidate into text and tests it against the patterns.
     * <p>
     * The text comes from the candidate itself, so a candidate with no useful string form
     * gives a filter that matches nothing sensible rather than one that fails. A candidate is
     * kept as soon as any one pattern matches, and the patterns are checked on a parallel
     * stream, which does not change the answer.
     * <p>
     * A script does not call this. The scanner calls it for each distinct state it walks
     * past, and a script reaches the filter behind it through a string filter step.
     * example:
     * <pre>
     * // this is the test behind a string filter step. The block helper is
     * // asked for its own text, which is a wrapper around the id, and the
     * // pattern has to allow for the wrapper around what it is looking for
     * const scanner = World.getWorldScanner()
     *   .withStringBlockFilter().matches(".*minecraft:.*_ore.*")
     *   .build();
     * const found = scanner.scanAroundPlayer(2);
     * Chat.log(`${found.size()} ore blocks, from ${scanner.getCachedAmount()} states tested`);
     * </pre>
     *
     * @param obj the candidate to turn into text and test
     * @return whether any of the patterns matched the text
     * @throws NullPointerException if {@code obj} is {@code null}, since its text is asked for
     * @since 1.6.5
     */
    @Override
    public Boolean apply(Object obj) {
        String toCompare = obj.toString();
        return filterObjects.parallelStream().anyMatch(filterElement -> filter.compare(toCompare, filterElement));
    }

}
