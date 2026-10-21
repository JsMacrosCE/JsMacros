package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.compare;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;

/**
 * Keeps candidates equal to a fixed character. Applying the filter to {@code null} throws
 * because the candidate is unboxed.
 *
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public class CharCompareFilter implements IFilter<Character> {

    private final char compareTo;

    /**
     * Creates a filter that keeps candidates equal to {@code compareTo}.
     *
     * @param compareTo the character a candidate has to equal to be kept
     * @since 1.6.5
     */
    public CharCompareFilter(char compareTo) {
        this.compareTo = compareTo;
    }

    /**
     * Tests whether the candidate equals the fixed character. A {@code null} candidate throws
     * when unboxed.
     *
     * @param character the candidate to test
     * @return whether the candidate equals the fixed character
     * @since 1.6.5
     */
    @Override
    public Boolean apply(Character character) {
        return character == compareTo;
    }

}
