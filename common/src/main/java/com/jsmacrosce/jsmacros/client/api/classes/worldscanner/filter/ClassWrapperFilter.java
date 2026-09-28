package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.compare.BooleanCompareFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.compare.CharCompareFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.compare.NumberCompareFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.compare.StringCompareFilter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * a filter that names a method on a helper, calls it, and compares whatever comes back.
 * <p>
 * This is what a {@code withBlockFilter} or {@code withStateFilter} step becomes. Rather than
 * asking the builder to hand over a function, a script names a method and says what to
 * compare, and this class looks the method up by reflection, calls it on each candidate, and
 * runs the result through whichever comparison suits the return type. The two concrete
 * subclasses fix the helper being reflected on, one for a block and one for a block state,
 * and differ in nothing else.
 * <p>
 * The method is looked up among the <b>declared</b> public methods of the helper that take
 * no parameters. That has three consequences that matter when picking a name. A method with
 * parameters is not a candidate, so a helper method that takes an argument cannot be named
 * at all. A method inherited from a superclass is not a candidate either, because it is not
 * declared on the helper, so the accessor the base helper class adds is unreachable this way
 * even though it is public. And a name that matches nothing leaves the lookup empty, so the
 * construction fails outright rather than producing a filter that quietly rejects everything.
 * <p>
 * What the arguments have to be is decided by the return type, and the count is not the same
 * for each. A boolean method takes one argument, the value to match, because there is nothing
 * to order. A char method takes one argument for the same reason. A string method takes two,
 * the name of the comparison and the text to compare against. A numeric method takes two as
 * well, the comparison operator and the number. A method returning anything else — another
 * helper, a list, an item stack — is refused outright, because there is no comparison to run
 * on it.
 * <br>
 * When a filter is applied, the method is invoked reflectively on the candidate and the
 * result is compared. A failure inside that call — a method that throws, or one that cannot
 * be reached — is caught, its stack trace is printed, and the candidate is treated as
 * rejected. That failure is silent from the script's point of view: the scan carries on and
 * the block simply does not appear, so a scan that returns nothing may be a broken method
 * name rather than an absence of matching blocks.
 * <br>
 * <b>This class is not in the shipped TypeScript definitions, and a script never names it.</b>
 * The only route is {@code World.getWorldScanner()}, and even there the script supplies a
 * method <i>name</i> as a string rather than a class.
 * example:
 * <pre>
 * // getHardness is a declared public method of the block helper taking no
 * // parameters, and it returns a float, so the filter takes an operator and
 * // a number. The block helper is what gets reflected on here
 * const scanner = World.getWorldScanner()
 *   .withBlockFilter("getHardness").is(">=", 10)
 *   .build();
 * Chat.log(`${scanner.scanAroundPlayer(2).size()} blocks with a hardness of 10 or more`);
 *
 * // a string method takes the name of a comparison and the text, which is
 * // how a block id is matched exactly. Reading getId rather than the whole
 * // stringified helper is what makes an exact match possible
 * const exact = World.getWorldScanner()
 *   .withBlockFilter("getId").is("EQUALS", "minecraft:stone")
 *   .build();
 * Chat.log(`${exact.scanAroundPlayer(2).size()} stone blocks`);
 *
 * // a boolean method takes only the value to match, since there is no order
 * // to compare against
 * const needsTool = World.getWorldScanner()
 *   .withBlockFilter("canMobSpawnInside").is(false)
 *   .build();
 * </pre>
 *
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
public abstract class ClassWrapperFilter<T> extends BasicFilter<T> {

    /**
     * the name of the method this filter calls, as it was written in the builder step.
     */
    protected final String methodName;
    /**
     * the resolved method, looked up once when the filter is built rather than on every test.
     */
    protected final Method method;
    /**
     * the arguments passed to that method, which is {@code null} for a call with none.
     */
    protected final Object[] methodArgs;

    /**
     * the comparison built from the return type of {@code method}, which decides how the
     * value the method produced is tested.
     */
    protected IFilter<?> filter;

    //TODO: Add a way to filter objects

    /**
     * resolves the method and builds the comparison for its return type.
     * <p>
     * The lookup and the return type check both happen here rather than per test, so a bad
     * method name is reported when the filter is built instead of when the scan runs. Neither
     * of those two checks covers the arguments themselves: a numeric operator is handed
     * straight to the comparison built for it, which stores it without looking at it, so a
     * mistyped one is not reported until a value is compared.
     *
     * @param methodName
     * @param methods
     * @param methodArgs the arguments that will be passed, when the specified method is invoked on the object
     * @param filterArgs the arguments for the filter
     * @throws NullPointerException if {@code methods} holds no entry for {@code methodName},
     *         which is what a name that is not a declared public parameterless method of the
     *         helper gives, and what a name matching only an inherited method or a method
     *         that takes parameters gives
     * @throws IllegalArgumentException if the resolved method returns a type that has no
     *         comparison, which is anything other than a boolean, a char, a string or a number
     * @throws ArrayIndexOutOfBoundsException if {@code filterArgs} holds fewer entries than
     *         the return type needs, which is one for a boolean or a char method and two for
     *         a string or a numeric one
     * @throws ClassCastException if an entry of {@code filterArgs} is of the wrong type for
     *         the slot the return type puts it in
     * @since 1.6.5
     */
    protected ClassWrapperFilter(String methodName, Map<String, Method> methods, Object[] methodArgs, Object[] filterArgs) {
        this.methodName = methodName;
        this.method = methods.get(methodName);
        this.methodArgs = methodArgs;
        this.filter = getFilter(method.getReturnType(), filterArgs);
    }

    /**
     * builds the comparison for a named method of a class, resolving both the method and the
     * comparison from the class itself.
     * <p>
     * This is the free standing entry point: it takes the class to reflect on rather than a
     * prebuilt lookup, and is what a caller uses when the helper is not one of the two the
     * concrete subclasses fix. A name the class declares no matching method for fails here
     * in the same way a bad name fails in the constructor.
     * <p>
     * The arguments given are the ones for the comparison, not for the method, because the
     * method is called with no parameters.
     * <p>
     * A script does not call this. It supplies a method name through a scanner builder step
     * instead, and the two subclasses here are what that reaches.
     * example:
     * <pre>
     * // the two arguments of a numeric filter are the operator and the
     * // number. The scanner builder fills in both halves of that from the
     * // is step, and this method is what turns a method name and those
     * // arguments into the comparison
     * const scanner = World.getWorldScanner()
     *   .withBlockFilter("getBlastResistance").is(">", 1000.0)
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} blast resistant blocks`);
     * </pre>
     *
     * @param clazz the class whose declared public parameterless methods are searched
     * @param methodName the name of the method whose return type decides the comparison
     * @param args the arguments for the comparison: one for a boolean or char return type,
     *             and two for a string or a numeric one
     * @return a comparison matching the return type of the named method
     * @throws NullPointerException if {@code clazz} declares no public parameterless method
     *         under {@code methodName}
     * @throws IllegalArgumentException if that method returns a type that has no comparison
     * @since 1.6.5
     */
    public static IFilter<?> getFilter(Class<?> clazz, String methodName, Object... args) {
        return getFilter(getPublicNoParameterMethods(clazz).get(methodName).getReturnType(), args);
    }

    /**
     * chooses the comparison for a return type.
     * <p>
     * The type decides which comparison is built and also how the arguments are read, which
     * is why the argument count differs per type. A primitive and its boxed counterpart are
     * treated alike, so a method declared to return an {@code int} and one declared to return
     * an {@link Integer} both get the numeric comparison. The precision used for an equality
     * test follows the type of the number the filter was given rather than the type of the
     * method, so the same method can be compared exactly against a whole number and loosely
     * against a decimal one.
     * <p>
     * A return type with no comparison is refused rather than matched loosely, which is the
     * clearest signal available that the method named in a builder step is not one that can
     * be filtered this way.
     *
     * @param returnType the return type of the method the filter will call
     * @param args the arguments for the comparison
     * @return a boolean comparison for a boolean return type, a character one for a char, a
     *         string one for a string, and a numeric one for any of the six number types and
     *         their boxed forms
     * @throws IllegalArgumentException if {@code returnType} is not one of those, with a
     *         message saying that methods returning objects besides a string are not
     *         currently supported
     * @since 1.6.5
     */
    private static IFilter<?> getFilter(Class<?> returnType, Object... args) {
        if (returnType == boolean.class || returnType == Boolean.class) {
            return new BooleanCompareFilter((boolean) args[0]);
        } else if (returnType == String.class) {
            return new StringCompareFilter((String) args[0], (String) args[1]);
        } else if (returnType == char.class || returnType == Character.class) {
            return new CharCompareFilter((char) args[0]);
        } else if (returnType == int.class || returnType == Integer.class
                || returnType == float.class || returnType == Float.class
                || returnType == double.class || returnType == Double.class
                || returnType == short.class || returnType == Short.class
                || returnType == long.class || returnType == Long.class
                || returnType == byte.class || returnType == Byte.class) {
            return new NumberCompareFilter((String) args[0], args[1]);
        } else {
            throw new IllegalArgumentException("Methods that return objects besides String are currently not supported");
        }
    }

    /**
     * calls the named method on the candidate and compares what it returns.
     * <p>
     * The call is reflective and the comparison is whichever one the return type called for.
     * A failure inside the call is caught, its stack trace is printed, and the candidate is
     * rejected, so a method that throws behaves as one that matched nothing rather than
     * aborting the scan. Nothing is remembered here: caching the verdict for a state that has
     * already been tested is the scanner's job, not the filter's.
     * <p>
     * A script does not call this. The scanner calls it for each distinct state it walks
     * past, and a script reaches the filter behind it by naming a method in a builder step.
     * example:
     * <pre>
     * // this is the call the scanner makes for each distinct state it meets,
     * // once per state rather than once per block. A method that throws
     * // prints a stack trace and the state is skipped, so a scan that comes
     * // back empty is not proof that nothing matched. getCachedAmount is how
     * // many states were actually tested
     * const scanner = World.getWorldScanner()
     *   .withBlockFilter("getHardness").is(">=", 10)
     *   .build();
     * const found = scanner.scanAroundPlayer(2);
     * Chat.log(`${found.size()} matches from ${scanner.getCachedAmount()} states tested`);
     * </pre>
     *
     * @param t the candidate the method is called on, which is a helper rather than a raw block
     * @return whether the value the method returned satisfies the comparison, or
     *         {@code false} if the call itself failed
     * @since 1.6.5
     */
    @Override
    public Boolean apply(T t) {
        try {
            return ((IFilter<Object>) filter).apply(method.invoke(t, methodArgs));
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * collects the public parameterless methods a class declares, keyed by name.
     * <p>
     * Declared rather than inherited, and parameterless only, which together make a name
     * that resolves here a safe one to pass to a builder step. Because the map is keyed by
     * name and only parameterless methods are kept, two methods sharing a name cannot
     * collide here: the ones taking parameters are filtered out first.
     *
     * @param clazz the class to inspect
     * @return the public parameterless methods declared by {@code clazz}, keyed by name
     * @since 1.6.5
     */
    protected static Map<String, Method> getPublicNoParameterMethods(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> method.getParameterCount() == 0)
                .collect(Collectors.toMap(Method::getName, p -> p));
    }

}
