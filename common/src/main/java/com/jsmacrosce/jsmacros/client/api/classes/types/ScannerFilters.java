package com.jsmacrosce.jsmacros.client.api.classes.types;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.BasicFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.compare.*;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.impl.*;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.logical.*;
import com.jsmacrosce.jsmacros.core.library.ScriptNamespace;

/**
 * Gives scripts filter types they can construct and combine through the {@code ScannerFilters} global.
 * <pre>
 * const filter = new ScannerFilters.Not(new ScannerFilters.StringCompare("EQUALS", "air"));
 * Chat.log(filter.apply("stone"));
 * </pre>
 */
@ScriptNamespace("ScannerFilters")
@DocletCategory("Script Type Namespaces")
@SuppressWarnings("rawtypes")
public final class ScannerFilters {
    private ScannerFilters() {
    }

    public static final Class<IFilter> Filter = IFilter.class;
    public static final Class<BasicFilter> Basic = BasicFilter.class;
    public static final Class<AndFilter> And = AndFilter.class;
    public static final Class<OrFilter> Or = OrFilter.class;
    public static final Class<XorFilter> Xor = XorFilter.class;
    public static final Class<NotFilter> Not = NotFilter.class;
    public static final Class<BooleanCompareFilter> BooleanCompare = BooleanCompareFilter.class;
    public static final Class<CharCompareFilter> CharCompare = CharCompareFilter.class;
    public static final Class<NumberCompareFilter> NumberCompare = NumberCompareFilter.class;
    public static final Class<StringCompareFilter> StringCompare = StringCompareFilter.class;
    public static final Class<StringifyFilter> Stringify = StringifyFilter.class;
    public static final Class<BlockFilter> Block = BlockFilter.class;
    public static final Class<BlockStateFilter> BlockState = BlockStateFilter.class;
}
