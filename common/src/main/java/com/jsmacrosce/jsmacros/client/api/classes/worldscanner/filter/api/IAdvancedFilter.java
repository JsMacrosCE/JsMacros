package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api;

import com.jsmacrosce.doclet.DocletCategory;

/**
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public interface IAdvancedFilter<T> extends IFilter<T> {

    IAdvancedFilter<T> and(IFilter<T> filter);

    IAdvancedFilter<T> or(IFilter<T> filter);

    IAdvancedFilter<T> xor(IFilter<T> filter);

    IAdvancedFilter<T> not();

}
