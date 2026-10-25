package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.impl;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.ClassWrapperFilter;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockHelper;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Calls a named public parameterless method on {@link BlockHelper} and compares its result.
 * The comparison arguments depend on the method's return type: boolean and char methods take
 * one value; string and numeric methods take a comparison name/operator and a value. Other
 * return types are unsupported.
 * <pre>
 * // getHardness returns a number, so the comparison takes an operator and target.
 * const scanner = World.getWorldScanner()
 *   .withBlockFilter("getHardness").is(">=", 10)
 *   .build();
 * const found = scanner.scanAroundPlayer(2);
 * Chat.log(`${found.size()} blocks with a hardness of 10 or more`);
 *
 * // String results take a comparison name and text.
 * const ores = World.getWorldScanner()
 *   .withBlockFilter("getId").is("ENDS_WITH", "ore")
 *   .build();
 * Chat.log(`${ores.scanAroundPlayer(2).size()} ore blocks`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
@DocletIgnore
public class BlockFilter extends ClassWrapperFilter<BlockHelper> {

    private static final Map<String, Method> METHOD_LOOKUP = getPublicNoParameterMethods(BlockHelper.class);

    /**
     * Builds a comparison for the named public parameterless {@link BlockHelper} method.
     *
     * @param methodName the name of a public parameterless method available on
     *                   {@link BlockHelper}
     * @param methodArgs arguments passed to the method, or {@code null} for none
     * @param filterArgs the arguments for the comparison, whose count and types are decided
     *                   by the return type of the named method
     * @throws NullPointerException if no eligible method has {@code methodName}; inherited
     *         public parameterless methods are included, except methods declared by Object
     * @throws IllegalArgumentException if the named method returns a type that has no
     *         comparison, such as another helper, a list or an item stack
     * @throws ArrayIndexOutOfBoundsException if {@code filterArgs} is shorter than the
     *         return type needs
     * @since 1.6.5
     */
    public BlockFilter(String methodName, Object[] methodArgs, Object[] filterArgs) {
        super(methodName, METHOD_LOOKUP, methodArgs, filterArgs);
    }

}
