package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api;

import com.jsmacrosce.doclet.DocletCategory;
/**
 * a two argument string test, the shape the string filters compare through.
 * <p>
 * Where a filter answers yes or no about one value, this answers about two, and it is
 * declared so the five string operations can be held in one field instead of being
 * reimplemented per filter. The method is the single abstract one, so the interface is a
 * functional interface and each of the operations is supplied as a method reference, which
 * is what {@link com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.compare.StringCompareFilter.FilterMethod}
 * does: one constant per operation, each holding the comparison it names.
 * <p>
 * The argument order is fixed and worth stating, because it is not the order the string
 * methods take. The first argument is the value being tested and the second is the value it
 * is tested against, so a filter holds the second one as its fixed target and hands it the
 * first as each candidate arrives.
 * <br>
 * <b>This interface is not in the shipped TypeScript definitions and a script never names
 * it.</b> Nothing constructs one from a script: the string filters build their comparisons
 * from a fixed set of method references, and a script picks between them by name through the
 * scanner builder rather than by supplying a comparison of its own. The string operations a
 * builder step can reach are {@code equals}, {@code contains}, {@code startsWith},
 * {@code endsWith} and {@code matches}, and they are plain delegations to the matching
 * method on {@link String}, so they carry that method's behaviour exactly, including the
 * fact that {@code matches} is a whole string match rather than a substring search.
 * example:
 * <pre>
 * // matches on a string filter step is a whole string match, so a pattern
 * // for part of a block id has to allow for the text around it. The block
 * // helper stringifies to BlockHelper:{"id": "minecraft:coal_ore"}, and the
 * // leading .* is what lets the pattern reach the id at all
 * const scanner = World.getWorldScanner()
 *   .withStringBlockFilter().matches(".*coal_ore.*")
 *   .build();
 * const found = scanner.scanAroundPlayer(2);
 * Chat.log(`${found.size()} coal ore blocks`);
 * </pre>
 *
 * @since 1.6.5
 */
@FunctionalInterface
@DocletCategory("Filters/Predicates")
public interface ICompare<T> {

    /**
     * compares the candidate against the fixed target.
     * <p>
     * A script does not call this. It is called by the string filters on every candidate
     * they test, against the string the filter was built with, and the result is what decides
     * whether the candidate is kept.
     * <p>
     * The implementations in use are the method references behind the string filter
     * operations, which delegate to the same named method on {@link String} and so inherit
     * its behaviour without adding any of their own.
     * example:
     * <pre>
     * // what a comparison does is visible through the step that selects it.
     * // endsWith reads the block id rather than the whole stringified helper,
     * // because it is applied to what getId returned
     * const scanner = World.getWorldScanner()
     *   .withBlockFilter("getId").is("ENDS_WITH", "ore")
     *   .build();
     * const found = scanner.scanAroundPlayer(2);
     * Chat.log(`${found.size()} blocks whose id ends in ore`);
     * </pre>
     *
     * @param obj1 the candidate being tested, which is the value that varies
     * @param obj2 the target the candidate is tested against, which the filter holds fixed
     * @return whether the candidate satisfies the comparison
     * @since 1.6.5
     */
    boolean compare(T obj1, T obj2);

}
