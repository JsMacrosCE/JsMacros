package com.jsmacrosce.jsmacros.client.access;

import com.jsmacrosce.doclet.DocletIgnore;
@DocletIgnore
public interface IPackedIntegerArray {

    long jsmacros_getMaxValue();

    int jsmacros_getElementsPerLong();

    int jsmacros_getIndexScale();

    int jsmacros_getIndexOffset();

    int jsmacros_getIndexShift();

}
