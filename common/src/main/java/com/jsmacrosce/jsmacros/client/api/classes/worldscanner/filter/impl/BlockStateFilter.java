package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.impl;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.ClassWrapperFilter;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Compares the result of a named public no-argument method on {@link BlockStateHelper}. Boolean
 * and character results take one comparison value; strings and numbers take an operator and a
 * value. Other return types are unsupported. Lookup includes inherited public methods but
 * excludes methods declared by {@link Object}.
 * <pre>
 * // isToolRequired is a declared public method of the state helper taking
 * // no parameters and returning a boolean, so the is step takes only the
 * // value to match. The state helper is the class this filter reflects on
 * const scanner = World.getWorldScanner()
 *   .withStateFilter("isToolRequired").is(false)
 *   .build();
 * const found = scanner.scanAroundPlayer(2);
 * Chat.log(`${found.size()} states that break without a tool`);
 *
 * // getLuminance returns an int, so the step takes an operator and a number
 * // instead. A string method would take the name of a comparison and text
 * const lit = World.getWorldScanner()
 *   .withStateFilter("getLuminance").is("==", 15)
 *   .build();
 * Chat.log(`${lit.scanAroundPlayer(2).size()} states emitting full light`);
 * </pre>
 *
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public class BlockStateFilter extends ClassWrapperFilter<BlockStateHelper> {

    private static final Map<String, Method> METHOD_LOOKUP = getPublicNoParameterMethods(BlockStateHelper.class);

    /**
     * Builds a comparison for the named public no-argument {@link BlockStateHelper} method.
     *
     * @param methodName the name of a public parameterless method available on
     *                   {@link BlockStateHelper}
     * @param methodArgs arguments passed to the method, or {@code null} for none
     * @param filterArgs the arguments for the comparison, whose count and types are decided
     *                   by the return type of the named method
     * @throws NullPointerException if no eligible method has {@code methodName}; inherited
     *         public parameterless methods are included, except methods declared by Object
     * @throws IllegalArgumentException if the named method returns a type that has no
     *         comparison, such as another helper or a block
     * @throws ArrayIndexOutOfBoundsException if {@code filterArgs} is shorter than the
     *         return type needs
     * @since 1.6.5
     */
    public BlockStateFilter(String methodName, Object[] methodArgs, Object[] filterArgs) {
        super(methodName, METHOD_LOOKUP, methodArgs, filterArgs);
    }

}
