package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.impl;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.ClassWrapperFilter;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * a filter that calls a named method on the block state helper and compares what it returns.
 * <p>
 * This is what a {@code withStateFilter}, {@code andStateFilter} or {@code orStateFilter}
 * step becomes when it names a method rather than using a string comparison. The only thing
 * it contributes is the choice of helper: it fixes the class that gets reflected on, and
 * hands the method name and the two argument lists to the base class, which does the lookup,
 * picks the comparison from the return type, and does the test. The set of names it accepts
 * is therefore the set of public parameterless methods <i>declared</i> by
 * {@link BlockStateHelper}, and what the arguments have to be is decided by which of those
 * the name refers to.
 * <p>
 * A state filter is a second stage rather than an alternative one. When a block filter is
 * also set, the scanner keeps a state only when the block filter and this one both accept it,
 * and the block filter is asked again for each state of that block. Naming a method that
 * takes arguments is not possible, which rules out the two state helper methods that do.
 * <p>
 * The method table is built once, when the class is initialised, rather than per filter, so
 * the reflection cost of a long chain of steps is paid once.
 * <br>
 * <b>This class is not in the shipped TypeScript definitions and a script never names it.</b>
 * The only route is {@code World.getWorldScanner()}, and even there a script supplies a
 * method <i>name</i> as a string rather than a class.
 * example:
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
     * resolves a method of the state helper and builds the comparison for its return type.
     * <p>
     * The method name is resolved against the state helper's own public parameterless
     * methods, and the arguments are the ones for the comparison rather than for the method,
     * which is called with no parameters.
     * <p>
     * A script never calls this. A {@code withStateFilter} step in a scanner builder does,
     * supplying the name written in the step along with the arguments from the {@code is}
     * that follows it.
     * example:
     * <pre>
     * // getPistonBehaviour is a declared public method of the state helper
     * // returning a string, so the arguments are the name of a comparison
     * // and the text to compare against
     * const scanner = World.getWorldScanner()
     *   .withStateFilter("getPistonBehaviour").is("EQUALS", "DESTROY")
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} states that break on push`);
     * </pre>
     *
     * @param methodName the name of a public parameterless method declared by
     *                   {@link BlockStateHelper}
     * @param methodArgs the arguments to call that method with, or {@code null} for none
     * @param filterArgs the arguments for the comparison, whose count and types are decided
     *                   by the return type of the named method
     * @throws NullPointerException if {@code methodName} is not a public parameterless method
     *         declared by {@link BlockStateHelper}, which includes a name matching only an
     *         inherited method or a method that takes parameters
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
