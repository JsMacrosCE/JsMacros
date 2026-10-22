package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.compare;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;

/**
 * a filter that keeps a boolean exactly when it equals one fixed value.
 * <p>
 * There is no ordering to apply to a boolean, so this is the simplest of the comparisons:
 * one value is fixed when the filter is built, and each candidate is tested for being equal
 * to it. The comparison runs the other way round from the way it reads — the candidate is
 * asked whether it equals the fixed value — which is what makes a {@code null} candidate
 * throw rather than quietly fail, since there is nothing to ask.
 * <p>
 * A script reaches this by naming a method that returns a boolean in a scanner builder step
 * and passing the value to match. A boolean method takes a single argument for that reason:
 * there is no operator to supply alongside it.
 * <br>
 * <b>This class is not in the shipped TypeScript definitions and a script never names it.</b>
 * The builder constructs it from the return type of the method named in the step.
 * example:
 * <pre>
 * // isToolRequired is a declared public method of the state helper taking
 * // no parameters and returning a boolean, so the step takes one argument:
 * // the value to match. Naming it false keeps the blocks that need no tool
 * const scanner = World.getWorldScanner()
 *   .withStateFilter("isToolRequired").is(false)
 *   .build();
 * const found = scanner.scanAroundPlayer(2);
 * Chat.log(`${found.size()} states that break without a tool`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public class BooleanCompareFilter implements IFilter<Boolean> {

    private final boolean compareTo;

    /**
     * fixes the value candidates are compared against.
     *
     * @param compareTo the value a candidate has to equal to be kept
     * @since 1.6.5
     */
    public BooleanCompareFilter(boolean compareTo) {
        this.compareTo = compareTo;
    }

    /**
     * tests a boolean against the fixed value.
     * <p>
     * This is an equality test and nothing more: there is no ordering, so no operator is
     * involved and no second argument.
     * <p>
     * A script does not call this. It is what a builder step naming a boolean method ends up
     * testing each candidate with.
     * example:
     * <pre>
     * // isAir returns a boolean, and the step passes the value to match as
     * // its only argument. Naming true keeps the air and nothing else
     * const air = World.getWorldScanner()
     *   .withStateFilter("isAir").is(true)
     *   .build();
     * Chat.log(`${air.scanAroundPlayer(2).size()} air states in range`);
     * </pre>
     *
     * @param bool the candidate to test
     * @return whether the candidate equals the fixed value
     * @throws NullPointerException if {@code bool} is {@code null}, since the test asks the
     *         candidate a question rather than the other way round
     * @since 1.6.5
     */
    @Override
    public Boolean apply(Boolean bool) {
        return bool.equals(compareTo);
    }

}
