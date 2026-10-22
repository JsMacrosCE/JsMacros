package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.compare;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;

/**
 * a filter that keeps a character exactly when it equals one fixed character.
 * <p>
 * The character counterpart of the boolean comparison: one value is fixed when the filter is
 * built and each candidate is tested for equality against it, with no ordering and therefore
 * no operator. The test unboxes the candidate before comparing, so a {@code null} candidate
 * throws rather than failing quietly.
 * <br>
 * <b>This class is not in the shipped TypeScript definitions, and no script can reach it.</b>
 * The builder chooses this comparison for a method whose return type is a character, and
 * neither helper the block and state filters reflect on declares a method returning one, so
 * in practice the branch that would build this filter is never taken. The class is public
 * and documented, but there is no builder step that produces it.
 * example:
 * <pre>
 * // there is no builder step that reaches this. A step naming a method
 * // would need that method to return a char, and neither the block helper
 * // nor the state helper declares one, so the filter is never built. A Java
 * // caller would write new CharCompareFilter(someChar)
 * Chat.log("a character filter is not reachable from a script");
 * </pre>
 *
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public class CharCompareFilter implements IFilter<Character> {

    private final char compareTo;

    /**
     * fixes the character candidates are compared against.
     *
     * @param compareTo the character a candidate has to equal to be kept
     * @since 1.6.5
     */
    public CharCompareFilter(char compareTo) {
        this.compareTo = compareTo;
    }

    /**
     * tests a character against the fixed one.
     * <p>
     * This is an equality test and nothing more. A script does not call it, and as noted
     * above no builder step produces the filter it belongs to.
     * example:
     * <pre>
     * // what a single character comparison would do, expressed through the
     * // filter a script can actually build for a single value. A boolean
     * // method also takes one argument, which is the same shape a character
     * // method would need
     * const scanner = World.getWorldScanner()
     *   .withStateFilter("isLiquid").is(true)
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} liquid states in range`);
     * </pre>
     *
     * @param character the candidate to test
     * @return whether the candidate equals the fixed character
     * @throws NullPointerException if {@code character} is {@code null}, since the comparison
     *         unboxes the candidate
     * @since 1.6.5
     */
    @Override
    public Boolean apply(Character character) {
        return character == compareTo;
    }

}
