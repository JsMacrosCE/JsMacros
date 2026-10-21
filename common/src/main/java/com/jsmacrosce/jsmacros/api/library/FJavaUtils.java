package com.jsmacrosce.jsmacros.api.library;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.core.Core;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;
import com.jsmacrosce.jsmacros.core.library.BaseLibrary;
import com.jsmacrosce.jsmacros.core.library.Library;

import java.util.*;

/**
 * Creates Java collections and provides array-string and raw-object helper conversions.
 * <pre>
 * // Equal seeds and equal calls produce equal sequences.
 * const random = JavaUtils.getRandom(1234);
 * Chat.log(`first roll was ${random.nextInt(100)}`);
 * </pre>
 * @author Etheradon
 * @since 1.8.4
 */
@Library("JavaUtils")
@SuppressWarnings("unused")
public class FJavaUtils extends BaseLibrary {

    /**
     * Initializes this library with its associated core.
     *
     * @param runner core associated with this library
     */
    public FJavaUtils(Core<?, ?> runner) {
        super(runner);
    }

    /**
     * Creates an empty {@link ArrayList}.
     *
     * @return an empty list.
     * @since 1.8.4
     */
    public ArrayList<?> createArrayList() {
        return new ArrayList<>();
    }

    /**
     * Copies the elements of a Java array into a new {@link ArrayList}.
     *
     * @param array the Java array to copy
     * @param <T>   the type of the array
     * @return a new list containing the array elements
     * @since 1.8.4
     */
    public <T> ArrayList<T> createArrayList(T[] array) {
        return Lists.newArrayList(array);
    }

    /**
     * Creates an empty {@link HashMap}.
     *
     * @return an empty map.
     * @since 1.8.4
     */
    public HashMap<?, ?> createHashMap() {
        return new HashMap<>();
    }

    /**
     * Creates an empty {@link HashSet}.
     *
     * @return an empty set.
     * @since 1.8.4
     */
    public HashSet<?> createHashSet() {
        return new HashSet<>();
    }

    /**
     * Returns a {@link SplittableRandom} initialized with a nondeterministic seed.
     *
     * @return a random number generator
     * @since 1.8.4
     */
    public SplittableRandom getRandom() {
        return new SplittableRandom();
    }

    /**
     * Returns a {@link SplittableRandom} initialized with {@code seed}; equal seeds and equal
     * sequences of calls produce equal results.
     * <pre>
     * // Equal seeds and equal calls produce equal sequences.
     * const first = JavaUtils.getRandom(1234);
     * const second = JavaUtils.getRandom(1234);
     * print(`${first.nextInt(100)}, ${first.nextInt(100)}, ${first.nextInt(100)}`);
     * print(`${second.nextInt(100)}, ${second.nextInt(100)}, ${second.nextInt(100)}`);
     * </pre>
     *
     * @param seed the seed
     * @return a seeded random number generator
     * @since 1.8.4
     */
    public SplittableRandom getRandom(long seed) {
        return new SplittableRandom(seed);
    }

    /**
     * Wraps {@code raw} with a registered helper for its runtime type, or returns {@code null}
     * if no helper is registered. {@code raw} must be non-null.
     *
     * @param raw the non-null object to wrap
     * @return its registered helper, or {@code null} if none is registered
     * @since 1.8.4
     */
    @Nullable
    public Object getHelperFromRaw(@NotNull Object raw) {
        Objects.requireNonNull(raw, "Object cannot be null.");
        return runner.helperRegistry.wrap(raw);
    }

    /**
     * Returns the string representation of an array; nested arrays are represented as objects
     * rather than recursively formatted.
     *
     * @param array the array to convert
     * @return the array's string representation
     * @since 1.8.4
     */
    public String arrayToString(Object[] array) {
        return Arrays.toString(array);
    }

    /**
     * Returns the string representation of an array, recursively formatting nested arrays.
     *
     * @param array the array to convert
     * @return the array's recursively formatted string representation
     * @since 1.8.4
     */
    public String arrayDeepToString(Object[] array) {
        return Arrays.deepToString(array);
    }

}
