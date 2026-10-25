package com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
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
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Calls a public no-argument method on each candidate helper and compares its result. Lookup
 * includes inherited public methods but excludes methods declared by {@link Object}. Boolean
 * and character results use one comparison argument; strings and numeric results use two.
 * Unsupported return types are rejected when the filter is constructed. Invocation failures
 * print a stack trace and reject that candidate.
 * <p>
 * The scanner builder uses concrete block and state filters; this supporting type is not
 * exposed in the shipped TypeScript definitions.
 * <pre>
 * // The method's numeric result determines the comparison arguments.
 * const scanner = World.getWorldScanner()
 *   .withBlockFilter("getHardness").is(">=", 10)
 *   .build();
 * Chat.log(`${scanner.scanAroundPlayer(2).size()} blocks with a hardness of 10 or more`);
 *
 * // A string result takes a comparison name and text.
 * const exact = World.getWorldScanner()
 *   .withBlockFilter("getId").is("EQUALS", "minecraft:stone")
 *   .build();
 * Chat.log(`${exact.scanAroundPlayer(2).size()} stone blocks`);
 *
 * // A boolean result takes only the value to match.
 * const needsTool = World.getWorldScanner()
 *   .withBlockFilter("canMobSpawnInside").is(false)
 *   .build();
 * </pre>
 *
 * @param <T> the type of candidate helpers evaluated by the filter
 * @author Etheradon
 * @since 1.6.5
 */
@DocletCategory("Filters/Predicates")
@DocletIgnore
public abstract class ClassWrapperFilter<T> extends BasicFilter<T> {

    /**
     * Name of the method this filter calls, as supplied to the builder.
     */
    protected final String methodName;
    /**
     * Method resolved when the filter is built.
     */
    protected final Method method;
    /**
     * Arguments passed to the method, or {@code null} when it takes none.
     */
    protected final Object[] methodArgs;

    /**
     * Comparison selected from {@code method}'s return type.
     */
    protected IFilter<?> filter;

    //TODO: Add a way to filter objects

    /**
     * Resolves the named method and builds a comparison for its return type.
     *
     * @param methodName name of the method to resolve
     * @param methods lookup of eligible methods
     * @param methodArgs arguments passed to the reflected method
     * @param filterArgs arguments used to build the comparison
     * @throws NullPointerException if {@code methods} has no entry for {@code methodName}; the
     *         message identifies the unknown filter method
     * @throws IllegalArgumentException if the resolved method returns a type that has no
     *         comparison, which is anything other than a boolean, a char, a string or a number
     * @throws ArrayIndexOutOfBoundsException if {@code filterArgs} holds fewer entries than
     *         the return type needs, which is one for a boolean or a char method and two for
     *         a string or a numeric one
     * @throws ClassCastException if an argument cannot be cast to the required type
     * @since 1.6.5
     */
    protected ClassWrapperFilter(String methodName, Map<String, Method> methods, Object[] methodArgs, Object[] filterArgs) {
        this.methodName = methodName;
        this.method = Objects.requireNonNull(methods.get(methodName), "Unknown filter method: " + methodName);
        this.methodArgs = methodArgs;
        this.filter = getFilter(method.getReturnType(), filterArgs);
    }

    /**
     * Builds a comparison for a public no-argument method found on {@code clazz}.
     *
     * @param clazz class whose public no-argument methods are searched
     * @param methodName the name of the method whose return type decides the comparison
     * @param args the arguments for the comparison: one for a boolean or char return type,
     *             and two for a string or a numeric one
     * @return a comparison matching the return type of the named method
     * @throws NullPointerException if no eligible method has {@code methodName}; the method
     *         lookup returns {@code null} before its return type is accessed
     * @throws IllegalArgumentException if that method returns a type that has no comparison
     * @since 1.6.5
     */
    public static IFilter<?> getFilter(Class<?> clazz, String methodName, Object... args) {
        return getFilter(getPublicNoParameterMethods(clazz).get(methodName).getReturnType(), args);
    }

    /**
     * Selects a comparison implementation based on the reflected method's return type.
     *
     * @param returnType the return type of the method the filter will call
     * @param args the arguments for the comparison
     * @return a boolean comparison for a boolean return type, a character one for a char, a
     *         string one for a string, and a numeric one for any of the six number types and
     *         their boxed forms
     * @throws IllegalArgumentException if {@code returnType} is not a supported primitive,
     *         boxed numeric/boolean/character type, or {@code String}
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
     * Invokes the method on the candidate and tests its result. An access or target-method
     * failure prints a stack trace and returns {@code false}.
     *
     * @param t candidate helper passed to the reflected method
     * @return whether the returned value satisfies the comparison, or {@code false} on an
     *         access or target-method failure
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
     * Collects public no-argument methods keyed by name, excluding methods declared by
     * {@link Object}. When names collide, the more-derived declaring class is preferred where
     * applicable.
     *
     * @param clazz the class to inspect
     * @return the public parameterless methods available on {@code clazz}, keyed by name
     * @since 1.6.5
     */
    protected static Map<String, Method> getPublicNoParameterMethods(Class<?> clazz) {
        return Arrays.stream(clazz.getMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> method.getDeclaringClass() != Object.class)
                .filter(method -> method.getParameterCount() == 0)
                .collect(Collectors.toMap(Method::getName, p -> p,
                        (first, second) -> first.getDeclaringClass().isAssignableFrom(second.getDeclaringClass()) ? second : first));
    }

}
