package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.impl;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.ClassWrapperFilter;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockHelper;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * a filter that calls a named method on the block helper and compares what it returns.
 * <p>
 * This is what a {@code withBlockFilter}, {@code andBlockFilter} or {@code orBlockFilter} step
 * becomes when it names a method rather than using a string comparison. The only thing it
 * contributes is the choice of helper: it fixes the class that gets reflected on, and hands
 * the method name and the two argument lists to the base class, which does the lookup, picks
 * the comparison from the return type, and does the test. The set of names it accepts is
 * therefore the set of public parameterless methods <i>declared</i> by {@link BlockHelper},
 * and what the arguments have to be is decided by which of those the name refers to.
 * <p>
 * The method table is built once, when the class is initialised, rather than per filter, so
 * the reflection cost of a long chain of steps is paid once.
 * <br>
 * <b>This class is not in the shipped TypeScript definitions and a script never names it.</b>
 * The only route is {@code World.getWorldScanner()}, and even there a script supplies a
 * method <i>name</i> as a string rather than a class.
 * example:
 * <pre>
 * // getHardness is a declared public method of the block helper taking no
 * // parameters and returning a float, so the is step takes an operator and
 * // a number. The block helper is the class this filter reflects on
 * const scanner = World.getWorldScanner()
 *   .withBlockFilter("getHardness").is(">=", 10)
 *   .build();
 * const found = scanner.scanAroundPlayer(2);
 * Chat.log(`${found.size()} blocks with a hardness of 10 or more`);
 *
 * // a string method takes the name of a comparison and the text instead,
 * // and getId is the method to use when an exact block id is wanted
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
public class BlockFilter extends ClassWrapperFilter<BlockHelper> {

    private static final Map<String, Method> METHOD_LOOKUP = getPublicNoParameterMethods(BlockHelper.class);

    /**
     * resolves a method of the block helper and builds the comparison for its return type.
     * <p>
     * The method name is resolved against the block helper's own public parameterless
     * methods, and the arguments are the ones for the comparison rather than for the method,
     * which is called with no parameters.
     * <p>
     * A script never calls this. A {@code withBlockFilter} step in a scanner builder does,
     * supplying the name written in the step along with the arguments from the {@code is}
     * that follows it.
     * example:
     * <pre>
     * // a numeric return type, so the arguments are an operator and a number.
     * // A boolean return type would take just the value to match, and a
     * // string one the name of a comparison and the text
     * const scanner = World.getWorldScanner()
     *   .withBlockFilter("getJumpVelocityMultiplier").is(">", 0.7)
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} blocks with a jump multiplier above 0.7`);
     * </pre>
     *
     * @param methodName the name of a public parameterless method available on
     *                   {@link BlockHelper}
     * @param methodArgs the arguments to call that method with, or {@code null} for none
     * @param filterArgs the arguments for the comparison, whose count and types are decided
     *                   by the return type of the named method
     * @throws NullPointerException if {@code methodName} is not a public parameterless method
     *         available on {@link BlockHelper}; inherited methods are included, but methods
     *         declared by Object and methods taking parameters are excluded
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
