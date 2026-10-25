package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;

import java.util.function.Function;

/**
 * A predicate that tests a value for acceptance. It is also a {@link Function} from
 * {@code T} to {@link Boolean}. This is a supporting interface, not a value scripts normally
 * hold: the fluent scanner builder creates filters internally, and custom predicates use
 * {@code World.getWorldScanner(JavaWrapper.methodToJava(...), ...)}. The
 * {@code withBlockFilter} and {@code withStateFilter} overloads accept a prebuilt filter for
 * Java callers and force sequential scanning.
 *
 * @param <T> the type of value tested by this predicate
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
@DocletIgnore
public interface IFilter<T> extends Function<T, Boolean> {

    /**
     * Tests whether a value is accepted. Implementations used by parallel scans may be called
     * concurrently; filters installed through the builder callback overloads force sequential
     * scanning.
     *
     * @param t the value to test
     * @return whether the value is accepted
     * @since 1.6.5
     */
    @Override
    Boolean apply(T t);

}
