package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.compare;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;

@DocletCategory("Filters/Predicates")
public class CharCompareFilter implements IFilter<Character> {

    private final char compareTo;

    public CharCompareFilter(char compareTo) {
        this.compareTo = compareTo;
    }

    @Override
    public Boolean apply(Character character) {
        return character == compareTo;
    }

}
