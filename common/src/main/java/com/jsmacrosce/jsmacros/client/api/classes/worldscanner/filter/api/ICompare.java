package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api;

import com.jsmacrosce.doclet.DocletCategory;

@FunctionalInterface
@DocletCategory("Filters/Predicates")
public interface ICompare<T> {

    boolean compare(T obj1, T obj2);

}
