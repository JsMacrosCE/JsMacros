package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.compare;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.ICompare;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;

/**
 * Compares string candidates with fixed text using {@code EQUALS}, {@code CONTAINS},
 * {@code STARTS_WITH}, {@code ENDS_WITH} or {@code MATCHES}. The operation name is
 * case-sensitive and resolved when the filter is constructed. {@code MATCHES} delegates to
 * {@link String#matches(String)}, which matches the entire candidate against the regular
 * expression.
 *
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public class StringCompareFilter implements IFilter<String> {

    private final String compareTo;

    protected final ICompare<String> filter;

    /**
     * Sets the comparison target and selects the operation.
     *
     * @param operation the comparison to run, spelled exactly as its constant:
     *                  {@code EQUALS}, {@code CONTAINS}, {@code STARTS_WITH},
     *                  {@code ENDS_WITH} or {@code MATCHES}
     * @param compareTo the text candidates are compared against
     * @throws IllegalArgumentException if {@code operation} is not one of those five exact,
     *         case-sensitive names
     * @throws NullPointerException if {@code operation} is {@code null}
     * @since 1.6.5
     */
    public StringCompareFilter(String operation, String compareTo) {
        this.compareTo = compareTo;
        filter = FilterMethod.valueOf(operation).getMethod();
    }

    /**
     * Tests a string candidate using the selected comparison.
     *
     * @param val the string to test
     * @return whether the comparison holds for {@code val}
     * @since 1.6.5
     */
    @Override
    public Boolean apply(String val) {
        return filter.compare(val, compareTo);
    }

    /**
     * The supported string comparison operations.
     *
     * @since 1.6.5
     */
    @DocletCategory("Filters/Predicates")
    public enum FilterMethod {
        /**
         * Keeps a value containing the fixed text anywhere within it.
         */
        CONTAINS(String::contains),
        /**
         * Keeps a value equal to the fixed text, case-sensitively.
         */
        EQUALS(String::equals),
        /**
         * Keeps a value beginning with the fixed text.
         */
        STARTS_WITH(String::startsWith),
        /**
         * Keeps a value ending with the fixed text.
         */
        ENDS_WITH(String::endsWith),
        /**
         * Keeps a value matched in full by the fixed regular expression.
         */
        MATCHES(String::matches);

        private final ICompare<String> method;

        FilterMethod(ICompare<String> method) {
            this.method = method;
        }

        /**
         * Returns the comparison represented by this operation.
         *
         * @return the comparison this constant names
         * @since 1.6.5
         */
        public ICompare<String> getMethod() {
            return method;
        }
    }

}
