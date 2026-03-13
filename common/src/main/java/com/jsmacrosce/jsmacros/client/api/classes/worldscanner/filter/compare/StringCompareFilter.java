package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.compare;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.ICompare;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;

/**
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public class StringCompareFilter implements IFilter<String> {

    private final String compareTo;

    protected final ICompare<String> filter;

    public StringCompareFilter(String operation, String compareTo) {
        this.compareTo = compareTo;
        filter = FilterMethod.valueOf(operation).getMethod();
    }

    @Override
    public Boolean apply(String val) {
        return filter.compare(val, compareTo);
    }

    @DocletCategory("Filters/Predicates")
    public enum FilterMethod {
        CONTAINS(String::contains),
        EQUALS(String::equals),
        STARTS_WITH(String::startsWith),
        ENDS_WITH(String::endsWith),
        MATCHES(String::matches);

        private final ICompare<String> method;

        FilterMethod(ICompare<String> method) {
            this.method = method;
        }

        public ICompare<String> getMethod() {
            return method;
        }
    }

}
