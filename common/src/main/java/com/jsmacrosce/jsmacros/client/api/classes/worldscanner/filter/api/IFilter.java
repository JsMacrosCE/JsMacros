package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api;

import com.jsmacrosce.doclet.DocletCategory;

import java.util.function.Function;

/**
 * the contract every world scanner filter implements: a predicate that takes one value and
 * says whether to keep it.
 * <p>
 * It extends {@link Function} of {@code T} to {@link Boolean}, so a filter is anything that
 * can be written as a lambda and the test itself is {@code apply}. The result is a boxed
 * {@link Boolean} rather than a primitive {@code boolean} because that is the shape the
 * function type fixes, and it is why the combining filters have to unbox it before they can
 * use it in a short circuiting operator.
 * <p>
 * How often a filter runs is worth knowing before writing one. The scanner tests each
 * <i>distinct block state</i> it walks past rather than each block, and caches the verdict,
 * so a state that appears a thousand times in a chunk is tested once. A block filter and a
 * state filter are combined with a logical and, and the block filter is re-evaluated for
 * every state of that block. A scanner also iterates its chunks on a parallel stream
 * whenever the filter it was handed allows it, and a filter built through the builder is
 * not a method wrapper, so it allows it — which means a filter of this kind can be called
 * from several threads at once and should not hold per scan state.
 * <br>
 * <b>None of this is reachable from a script by name.</b> This interface is not in the
 * shipped TypeScript definitions, and a script has no way to construct a filter either: the
 * only route is {@code World.getWorldScanner()}, which returns a builder that creates the
 * implementation classes internally and hands the finished filter straight to the scanner.
 * What a script writes is the chain, and the chain picks the class. Naming a block or state
 * method makes a filter that reflects on the helper and compares the return value; a string
 * comparison makes one that stringifies the helper; and an {@code and}, {@code or} or
 * {@code not} step wraps what came before in one of the logical filters.
 * example:
 * <pre>
 * // the chain, not the filter, is what a script actually writes. Naming a
 * // method on the block helper makes a filter that calls it and compares
 * // what comes back; getHardness returns a float, so the arguments are an
 * // operation and the number to compare against
 * const scanner = World.getWorldScanner()
 *   .withBlockFilter("getHardness").is(">=", 10)
 *   .build();
 * // build hands the filter to the scanner, which keeps the states it accepts
 * const found = scanner.scanAroundPlayer(2);
 * Chat.log(`${found.size()} blocks with a hardness of 10 or more`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public interface IFilter<T> extends Function<T, Boolean> {

    /**
     * tests one value.
     * <p>
     * An implementation is free to be as expensive as it likes, because the scanner calls it
     * once per distinct state and caches the answer, but it is called off the main thread
     * when the scan is parallel, so it must not touch the world.
     * <p>
     * A script does not call this directly. It is reached through the chain on
     * {@code World.getWorldScanner()}, which is where the filter behind each step is built.
     * example:
     * <pre>
     * // the same filter expressed as the chain that makes it: a state filter
     * // naming a boolean method takes just the value to match, with no
     * // operation, because there is nothing to order
     * const scanner = World.getWorldScanner()
     *   .withStateFilter("isAir").is(false)
     *   .build();
     * const found = scanner.scanAroundPlayer(2);
     * Chat.log(`${found.size()} non-air states in range`);
     * </pre>
     *
     * @param t the value to test, which is a block or a block state depending on which
     *          filter this is
     * @return whether the value is kept
     * @since 1.6.5
     */
    @Override
    Boolean apply(T t);

}
