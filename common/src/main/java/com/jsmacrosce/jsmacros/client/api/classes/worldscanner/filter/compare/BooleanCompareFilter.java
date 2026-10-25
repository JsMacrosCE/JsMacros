package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.compare;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;

/**
 * Keeps candidates from a named boolean-returning method when they equal the fixed value.
 * The scanner builder creates this comparison automatically; construct it directly only when
 * testing values you already have.
 *
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
@DocletIgnore
public class BooleanCompareFilter implements IFilter<Boolean> {

    private final boolean compareTo;

    /**
     * Sets the value a candidate must equal to be kept.
     *
     * @param compareTo the value a candidate has to equal to be kept
     * @since 1.6.5
     */
    public BooleanCompareFilter(boolean compareTo) {
        this.compareTo = compareTo;
    }

    /**
     * Tests whether the candidate equals the fixed value; a {@code null} candidate throws
     * {@link NullPointerException}.
     *
     * @param bool the candidate to test
     * @return whether the candidate equals the fixed value
     * @since 1.6.5
     */
    @Override
    public Boolean apply(Boolean bool) {
        return bool.equals(compareTo);
    }

}
