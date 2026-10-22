package com.jsmacrosce.jsmacros.client.api.classes.worldscanner;

import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IAdvancedFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.api.IFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.BasicFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.impl.BlockFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.impl.BlockStateFilter;
import com.jsmacrosce.jsmacros.client.api.classes.worldscanner.filter.impl.StringifyFilter;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockStateHelper;
import com.jsmacrosce.jsmacros.client.api.library.impl.FWorld;
import com.jsmacrosce.jsmacros.core.MethodWrapper;

/**
 * Builds scanners with separate block and block-state filters. Named-method and string filters run in the scanner's native filtering path;
 * callback filters force sequential scanning. A filter step starts with a {@code with}, {@code and}, {@code or}, or {@code xor} method and
 * is completed by a comparison call; {@code not} immediately negates the complete existing filter. A second {@code with} filter replaces
 * the existing filter in that category, while the other operators combine with it. A pending step must be completed before another starts.
 * String filters inspect the helper's complete {@code toString()} text, and each supplied pattern is an alternative. {@link #matches(String[])}
 * uses whole-string regular-expression matching; the other string operations compare literal text. Use a named-method filter such as
 * {@link #withBlockFilter(String)} with {@code "getId"} to compare a method's returned value instead.
 * <p>
 * For example, this finds blocks whose helper text contains {@code _ore} or {@code _block}:
 * <pre>
 * // matches applies each regular expression to the complete helper text
 * const ores = World.getWorldScanner()
 *   .withStringBlockFilter().matches(".*_ore.*")
 *   .orStringBlockFilter().matches(".*_block.*")
 *   .build();
 * Chat.log(`${ores.scanAroundPlayer(2).size()} ore or building blocks`);
 * </pre>
 * <p>
 * Named-method filters accept public no-argument methods on {@link BlockStateHelper} or {@link BlockHelper}, except methods declared by
 * {@code Object}; their return values must be numeric, String, or boolean. Numeric comparisons use an operator and a value, String
 * comparisons use a comparison name and text, and boolean comparisons take the expected value. For more complex filters, use
 * {@link FWorld#getWorldScanner(MethodWrapper, MethodWrapper)}.
 *
 * @author Etheradon
 * @since 1.6.5
 */
@SuppressWarnings("unused")
public final class WorldScannerBuilder {

    @Nullable
    private IAdvancedFilter<BlockHelper> blockFilter;
    @Nullable
    private IAdvancedFilter<BlockStateHelper> stateFilter;

    private FilterCategory selectedCategory;
    private Operation operation;
    private String method;
    private boolean sequential;

    /**
     * Creates a builder with no block or block-state filters; scanners built without adding filters accept all blocks and states.
     *
     * @since 1.6.5
     */
    public WorldScannerBuilder() {
        selectedCategory = FilterCategory.NONE;
        operation = Operation.NONE;
    }

    @Nullable
    private IAdvancedFilter<?> getTargetFilter() {
        if (selectedCategory == FilterCategory.BLOCK) {
            return blockFilter;
        } else if (selectedCategory == FilterCategory.STATE) {
            return stateFilter;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void setTargetFilter(@Nullable IAdvancedFilter<?> filter) {
        if (selectedCategory == FilterCategory.BLOCK) {
            blockFilter = (IAdvancedFilter<BlockHelper>) filter;
        } else if (selectedCategory == FilterCategory.STATE) {
            stateFilter = (IAdvancedFilter<BlockStateHelper>) filter;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void composeFilters(@Nullable IFilter<T> filter) {
        if (selectedCategory == null || selectedCategory == FilterCategory.NONE) {
            throw new IllegalStateException("No category for creating the new filter was specified.");
        } else {
            if (operation == Operation.NEW) {
                if (selectedCategory == FilterCategory.BLOCK) {
                    blockFilter = (IAdvancedFilter<BlockHelper>) filter;
                } else if (selectedCategory == FilterCategory.STATE) {
                    stateFilter = (IAdvancedFilter<BlockStateHelper>) filter;
                }
            } else {
                IAdvancedFilter<T> target = (IAdvancedFilter<T>) getTargetFilter();
                if (target == null) {
                    throw new IllegalStateException("Can't compose null filters.");
                }
                switch (operation) {
                    case OR:
                        setTargetFilter(target.or(filter));
                        break;
                    case AND:
                        setTargetFilter(target.and(filter));
                        break;
                    case XOR:
                        setTargetFilter(target.xor(filter));
                        break;
                    case NOT:
                        setTargetFilter(target.not());
                        break;
                    default:
                        throw new IllegalStateException("Unknown operation for combining filters");
                }
            }
            finishFilter();
        }
    }

    private boolean canCreateNewFilter() {
        return selectedCategory == FilterCategory.NONE;
    }

    private void createNewFilter(Operation operation, FilterCategory category, String method) {
        if (canCreateNewFilter()) {
            this.operation = operation;
            this.selectedCategory = category;
            this.method = method;
        } else {
            throw new IllegalStateException("Can't create a new filter, because the old one is not completed.");
        }
    }

    private void finishFilter() {
        if (selectedCategory != FilterCategory.NONE) {
            operation = Operation.NONE;
            selectedCategory = FilterCategory.NONE;
            method = "";
        } else {
            throw new IllegalStateException("Can't complete filter, because there is none.");
        }
    }

    /**
     * Starts a block-state filter using a named helper method. Complete it with a comparison call; the completed {@code with} filter replaces
     * any existing state filter. A pending filter step prevents another from starting.
     * <pre>
     * // A boolean-returning method takes the expected value as its comparison
     * const scanner = World.getWorldScanner()
     *   .withStateFilter("isToolRequired").is(false)
     *   .build();
     * const found = scanner.scanAroundPlayer(2);
     * Chat.log(`${found.size()} states that break without a tool`);
     * </pre>
     *
     * @param method the name of a public no-argument method on {@link BlockStateHelper}
     * @return this builder, for chaining
     * @throws IllegalStateException if another start step is already waiting to be completed
     * @since 1.6.5
     */
    public WorldScannerBuilder withStateFilter(String method) {
        createNewFilter(Operation.NEW, FilterCategory.STATE, method);
        return this;
    }

    /**
     * Installs a completed callback filter for block states and forces sequential scanning.
     *
     * @param filter the block-state callback filter
     * @return this builder for chaining
     * @throws IllegalStateException if a filter step is pending
     */
    public WorldScannerBuilder withStateFilter(IFilter<BlockStateHelper> filter) {
        if (!canCreateNewFilter()) throw new IllegalStateException("Complete the pending filter before replacing it");
        stateFilter = new BasicFilter<>() {
            @Override
            public Boolean apply(BlockStateHelper state) {
                return filter.apply(state);
            }
        };
        sequential = true;
        return this;
    }

    /**
     * Starts a named-method state filter to AND with the existing state filter. Complete it with
     * {@link #is(Object[], Object[])} or a string comparison; both filters must accept the state.
     * Completion fails if no state filter exists.
     * <pre>
     * // getLuminance returns a number, so its comparison takes an operator and a number
     * const scanner = World.getWorldScanner()
     *   .withStateFilter("isToolRequired").is(false)
     *   .andStateFilter("getLuminance").is("==", 15)
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} unbreakable fully lit states`);
     * </pre>
     *
     * @param method the name of a public no-argument method available on
     *               {@link BlockStateHelper}
     * @return this builder, for chaining
     * @throws IllegalStateException if another start step is pending, or if no state filter exists
     * @since 1.6.5
     */
    public WorldScannerBuilder andStateFilter(String method) {
        createNewFilter(Operation.AND, FilterCategory.STATE, method);
        return this;
    }

    /**
     * Starts a named-method state filter to OR with the existing state filter. Complete it with
     * {@link #is(Object[], Object[])} or a string comparison; either filter may accept the state.
     * Completion fails if no state filter exists.
     * <pre>
     * // getId returns a string, so compare it with a string operation and value
     * const scanner = World.getWorldScanner()
     *   .withStateFilter("isSolid").is(true)
     *   .orStateFilter("getId").is("ENDS_WITH", "torch")
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} solid states or torches`);
     * </pre>
     *
     * @param method the name of a public no-argument method available on
     *               {@link BlockStateHelper}
     * @return this builder, for chaining
     * @throws IllegalStateException if another start step is pending, or if no state filter exists
     * @since 1.6.5
     */
    public WorldScannerBuilder orStateFilter(String method) {
        createNewFilter(Operation.OR, FilterCategory.STATE, method);
        return this;
    }

    /**
     * Starts a named-method state filter to XOR with the existing state filter. Complete it with
     * {@link #is(Object[], Object[])} or a string comparison; exactly one filter must accept the
     * state. Completion fails if no state filter exists.
     *
     * @param method the name of a public no-argument method available on {@link BlockStateHelper}
     * @return this builder for chaining
     * @throws IllegalStateException if a filter step is pending
     */
    public WorldScannerBuilder xorStateFilter(String method) {
        createNewFilter(Operation.XOR, FilterCategory.STATE, method);
        return this;
    }

    /**
     * Immediately negates the entire existing state filter, keeping states it rejected. It takes no
     * method name and throws if no state filter exists or another start step is pending.
     * <pre>
     * // Negate the completed state filter
     * const scanner = World.getWorldScanner()
     *   .withStateFilter("exceedsCube").is(false)
     *   .notStateFilter()
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} states that are not full cubes`);
     * </pre>
     *
     * @return this builder, for chaining
     * @throws IllegalStateException if the state filter has not been completed or another start
     *         step is pending
     * @since 1.6.5
     */
    public WorldScannerBuilder notStateFilter() {
        createNewFilter(Operation.NOT, FilterCategory.STATE, "");
        composeFilters(null);
        return this;
    }

    /**
     * Starts a block filter on a named method; complete it with {@link #is(Object[], Object[])} or a
     * string comparison. Invalid method names and comparison arguments are checked during completion.
     * Starting another step while one is pending throws {@code IllegalStateException}.
     * <p>
     * A block filter and a state filter are two independent stages rather than alternatives, so a
     * state filter set afterwards narrows the block filter rather than replacing it.
     * <pre>
     * // getHardness returns a number, so its comparison takes an operator and a number
     * const scanner = World.getWorldScanner()
     *   .withBlockFilter("getHardness").is(">=", 10)
     *   .build();
     * const found = scanner.scanAroundPlayer(2);
     * Chat.log(`${found.size()} blocks with a hardness of 10 or more`);
     * </pre>
     *
     * @param method the name of a public no-argument method available on {@link BlockHelper}, and
     *               the arguments the following {@link #is(Object[], Object[])} needs are decided
     *               by its return type
     * @return this builder, for chaining
     * @throws IllegalStateException if another start step is already waiting to be completed
     * @since 1.6.5
     */
    public WorldScannerBuilder withBlockFilter(String method) {
        createNewFilter(Operation.NEW, FilterCategory.BLOCK, method);
        return this;
    }

    /**
     * Installs a completed callback filter for blocks and forces sequential scanning. This replaces
     * any existing block filter.
     *
     * @param filter the block callback filter
     * @return this builder for chaining
     * @throws IllegalStateException if a filter step is pending
     */
    public WorldScannerBuilder withBlockFilter(IFilter<BlockHelper> filter) {
        if (!canCreateNewFilter()) throw new IllegalStateException("Complete the pending filter before replacing it");
        blockFilter = new BasicFilter<>() {
            @Override
            public Boolean apply(BlockHelper block) {
                return filter.apply(block);
            }
        };
        sequential = true;
        return this;
    }

    /**
     * Starts a named-method block filter to AND with the existing block filter. Complete it with
     * {@link #is(Object[], Object[])} or a string comparison; both filters must accept the block.
     * Completion fails if no block filter exists.
     * <pre>
     * // Both comparisons are numeric; both conditions must accept the block
     * const scanner = World.getWorldScanner()
     *   .withBlockFilter("getHardness").is(">=", 10)
     *   .andBlockFilter("getBlastResistance").is(">", 1200)
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} hard and blast resistant blocks`);
     * </pre>
     *
     * @param method the name of a public no-argument method available on {@link BlockHelper}
     * @return this builder, for chaining
     * @throws IllegalStateException if the block filter has not been started yet, or if a start
     *         step is already waiting to be completed
     * @since 1.6.5
     */
    public WorldScannerBuilder andBlockFilter(String method) {
        createNewFilter(Operation.AND, FilterCategory.BLOCK, method);
        return this;
    }

    /**
     * Starts a named-method block filter to OR with the existing block filter. Complete it with
     * {@link #is(Object[], Object[])} or a string comparison; the method's return type determines
     * the comparison arguments. Completion fails if no block filter exists.
     *
     * @param method the name of a public no-argument method available on {@link BlockHelper}
     * @return this builder, for chaining
     * @throws IllegalStateException if another start step is pending
     * @since 1.6.5
     */
    public WorldScannerBuilder orBlockFilter(String method) {
        createNewFilter(Operation.OR, FilterCategory.BLOCK, method);
        return this;
    }

    /**
     * Starts a named-method block filter to XOR with the existing block filter. Complete it with a
     * comparison call; exactly one constituent must accept. The method's return type determines
     * the comparison arguments. Completion fails if no block filter exists.
     *
     * @param method a public no-argument method, including inherited methods
     * @return this builder for chaining
     * @throws IllegalStateException if another start step is pending
     */
    public WorldScannerBuilder xorBlockFilter(String method) {
        createNewFilter(Operation.XOR, FilterCategory.BLOCK, method);
        return this;
    }

    /**
     * Immediately negates the entire existing block filter. This completes within this call and
     * throws if no block filter exists or another start step is pending.
     *
     * @return this builder, for chaining
     * @throws IllegalStateException if the block filter has not been started, which is the
     *         {@code "Can't compose null filters."} case, or if a start step is already waiting to
     *         be completed
     * @since 1.6.5
     */
    public WorldScannerBuilder notBlockFilter() {
        createNewFilter(Operation.NOT, FilterCategory.BLOCK, "");
        composeFilters(null);
        return this;
    }

    /**
     * Starts a block filter that compares the block helper's complete {@code toString()} text.
     * Complete it with {@link #equals(String[])}, {@link #contains(String[])},
     * {@link #startsWith(String[])}, {@link #endsWith(String[])} or {@link #matches(String[])}.
     *
     * @return this builder, for chaining
     * @throws IllegalStateException if a start step is already waiting to be completed
     * @since 1.6.5
     */
    public WorldScannerBuilder withStringBlockFilter() {
        createNewFilter(Operation.NEW, FilterCategory.BLOCK, "");
        return this;
    }

    /**
     * Starts a string block filter to AND with the existing block filter. Complete it with a string
     * comparison; both filters must accept the block. Completion fails if no block filter exists.
     *
     * @return this builder, for chaining
     * @throws IllegalStateException if the block filter has not been started yet, or if a start
     *         step is already waiting to be completed
     * @since 1.6.5
     */
    public WorldScannerBuilder andStringBlockFilter() {
        createNewFilter(Operation.AND, FilterCategory.BLOCK, "");
        return this;
    }

    /**
     * Starts a string block filter to OR with the existing block filter. Complete it with a string
     * comparison; either filter may accept the block. Completion fails if no block filter exists.
     *
     * @return this builder, for chaining
     * @throws IllegalStateException if the block filter has not been started yet, or if a start
     *         step is already waiting to be completed
     * @since 1.6.5
     */
    public WorldScannerBuilder orStringBlockFilter() {
        createNewFilter(Operation.OR, FilterCategory.BLOCK, "");
        return this;
    }

    /**
     * Starts a string block filter to be XORed with the existing block filter. Complete it with a
     * string comparison; exactly one constituent must accept.
     *
     * @return this builder for chaining
     * @throws IllegalStateException if another start step is pending
     */
    public WorldScannerBuilder xorStringBlockFilter() {
        createNewFilter(Operation.XOR, FilterCategory.BLOCK, "");
        return this;
    }

    /**
     * Starts a block-state filter that compares the state helper's complete {@code toString()} text,
     * including its property map. Complete it with {@link #equals(String[])},
     * {@link #contains(String[])}, {@link #startsWith(String[])}, {@link #endsWith(String[])} or
     * {@link #matches(String[])}.
     *
     * @return this builder, for chaining
     * @throws IllegalStateException if a start step is already waiting to be completed
     * @since 1.6.5
     */
    public WorldScannerBuilder withStringStateFilter() {
        createNewFilter(Operation.NEW, FilterCategory.STATE, "");
        return this;
    }

    /**
     * Starts a string state filter to AND with the existing state filter. Complete it with a
     * string comparison; both filters must accept the state.
     *
     * @return this builder, for chaining
     * @throws IllegalStateException if the state filter has not been started yet, or if a start
     *         step is already waiting to be completed
     * @since 1.6.5
     */
    public WorldScannerBuilder andStringStateFilter() {
        createNewFilter(Operation.AND, FilterCategory.STATE, "");
        return this;
    }

    /**
     * Starts a string state filter to OR with the existing state filter. Complete it with a
     * string comparison; either filter may accept the state.
     *
     * @return this builder, for chaining
     * @throws IllegalStateException if the state filter has not been started yet, or if a start
     *         step is already waiting to be completed
     * @since 1.6.5
     */
    public WorldScannerBuilder orStringStateFilter() {
        createNewFilter(Operation.OR, FilterCategory.STATE, "");
        return this;
    }

    /**
     * Starts a string state filter to be XORed with the existing state filter. Complete it with a
     * string comparison; exactly one constituent must accept.
     *
     * @return this builder for chaining
     * @throws IllegalStateException if a filter step is pending
     */
    public WorldScannerBuilder xorStringStateFilter() {
        createNewFilter(Operation.XOR, FilterCategory.STATE, "");
        return this;
    }

    /**
     * Completes the pending named-method filter using {@code args} as the comparison arguments.
     * Boolean and character methods take one value; String and numeric methods take two (a
     * comparison name or operator, then a value). Numeric operators are interpreted when candidates
     * are tested, so an invalid operator rejects candidates rather than failing at construction.
     * <pre>
     * // is(">=", 10) is the whole argument list: an operator and a number,
     * // because getHardness returns a float
     * const scanner = World.getWorldScanner()
     *   .withBlockFilter("getHardness").is(">=", 10)
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} blocks of hardness 10 or more`);
     * </pre>
     *
     * @param args the arguments for the comparison, whose count and types are decided by the
     *             return type of the method the pending step named
     * @return this builder, for chaining
     * @throws IllegalStateException if no start step is waiting to be completed
     * @throws NullPointerException if the named method is not a public no-argument method
     *         available on the helper for the pending category, including inherited methods
     * @throws IllegalArgumentException if the named method returns a type the comparison cannot
     *         handle, or, for a String method, if the comparison name is not one of the five
     * @throws ClassCastException if the first argument is not a string, which is what happens to a
     *         numeric method given a single number
     * @throws ArrayIndexOutOfBoundsException if there are fewer arguments than the return type
     *         needs, which is none at all for a boolean method and one for a numeric or String one
     * @since 1.6.5
     */
    public WorldScannerBuilder is(Object... args) {
        return is(null, args);
    }

    /**
     * Completes the pending named-method filter, passing {@code methodArgs} to that method and
     * {@code filterArgs} to its comparison. The helpers' eligible methods take no arguments; use
     * {@link #is(Object[])} to supply comparison arguments for those methods.
     *
     * @param methodArgs the arguments to call the named method with, or {@code null} for none
     * @param filterArgs the arguments for the comparison, whose count and types are decided by the
     *                   return type of the named method
     * @return this builder, for chaining
     * @throws IllegalStateException if no start step is waiting to be completed
     * @throws NullPointerException if the named method is not a public no-argument method
     *         available on the helper for the pending category, including inherited methods
     * @throws IllegalArgumentException if the named method returns an unsupported type
     * @throws ClassCastException if a comparison argument has the wrong type
     * @throws ArrayIndexOutOfBoundsException if {@code filterArgs} has too few entries
     * @since 1.6.5
     */
    public WorldScannerBuilder is(Object[] methodArgs, Object[] filterArgs) {
        if (selectedCategory == FilterCategory.STATE) {
            composeFilters(new BlockStateFilter(method, methodArgs, filterArgs));
        } else if (selectedCategory == FilterCategory.BLOCK) {
            composeFilters(new BlockFilter(method, methodArgs, filterArgs));
        } else {
            throw new IllegalStateException("Can't complete filter, because there is none.");
        }
        return this;
    }

    /**
     * Alias of {@link #is(Object[])} for languages in which {@code is} is a keyword. This alias is
     * not exposed by the typed builder returned by {@code World.getWorldScanner()}.
     *
     * @param args the arguments for the comparison, whose count and types are decided by the
     *             return type of the method the pending step named
     * @return this builder, for chaining
     * @throws IllegalStateException if no start step is waiting to be completed
     * @throws NullPointerException if the named method is not a public no-argument method
     *         available on the helper for the pending category, including inherited methods
     * @throws IllegalArgumentException if the named method returns a type the comparison cannot
     *         handle, or, for a String method, if the comparison name is not one of the five
     * @throws ClassCastException if the first argument is not a string, which is what happens to a
     *         numeric method given a single number
     * @throws ArrayIndexOutOfBoundsException if there are fewer arguments than the return type
     *         needs, which is none at all for a boolean method and one for a numeric or String one
     * @since 1.6.5
     */
    public WorldScannerBuilder test(Object... args) {
        return is(args);
    }

    /**
     * Alias of {@link #is(Object[], Object[])} for languages in which {@code is} is a keyword. No
     * helper method accepts arguments, so this overload has no useful named-method arguments; it is
     * also absent from the typed builder returned by {@code World.getWorldScanner()}.
     *
     * @param methodArgs the arguments to call the named method with, or {@code null} for none
     * @param filterArgs the arguments for the comparison, whose count and types are decided by the
     *                   return type of the named method
     * @return this builder, for chaining
     * @throws IllegalStateException if no start step is waiting to be completed
     * @throws NullPointerException if the named method is not a public no-argument method
     *         available on the helper for the pending category, including inherited methods
     * @throws IllegalArgumentException if the named method returns a type the comparison cannot
     *         handle, or, for a String method, if the comparison name is not one of the five
     * @throws ClassCastException if the first argument is not a string, which is what happens to a
     *         numeric method given a single number
     * @throws ArrayIndexOutOfBoundsException if there are fewer arguments than the return type
     *         needs, which is none at all for a boolean method and one for a numeric or String one
     * @since 1.6.5
     */
    public WorldScannerBuilder test(Object[] methodArgs, Object[] filterArgs) {
        return is(methodArgs, filterArgs);
    }

    /**
     * Completes a pending string filter by keeping a candidate when its text equals any supplied
     * pattern. The candidate text is the helper's string form, not just an identifier; an empty
     * pattern list therefore matches nothing.
     *
     * @param args the patterns to compare the text against, at least one of which has to be equal
     *             for a candidate to be kept
     * @return this builder, for chaining
     * @throws IllegalStateException if no start step is waiting to be completed
     * @since 1.6.5
     */
    public WorldScannerBuilder equals(String... args) {
        createStringFilter("EQUALS", args);
        return this;
    }

    /**
     * Completes a pending string filter by keeping a candidate when its text contains any supplied
     * pattern as a literal substring. The candidate text is the helper's string form, and the
     * patterns are alternatives; use {@link #matches(String[])} for regular-expression matching.
     *
     * @param args the substrings to look for in the text, at least one of which has to be found for
     *             a candidate to be kept
     * @return this builder, for chaining
     * @throws IllegalStateException if no start step is waiting to be completed
     * @since 1.6.5
     */
    public WorldScannerBuilder contains(String... args) {
        createStringFilter("CONTAINS", args);
        return this;
    }

    /**
     * Completes a pending string filter, keeping a candidate when its helper text starts with any
     * supplied prefix. The patterns are alternatives; they are matched against the complete helper
     * string, not a named method's return value.
     *
     * @param args the prefixes the text has to begin with, at least one of which has to match for a
     *             candidate to be kept
     * @return this builder, for chaining
     * @throws IllegalStateException if no start step is waiting to be completed
     * @since 1.6.5
     */
    public WorldScannerBuilder startsWith(String... args) {
        createStringFilter("STARTS_WITH", args);
        return this;
    }

    /**
     * Completes a pending string filter, keeping a candidate when its helper text ends with any
     * supplied suffix. The patterns are alternatives and are matched against the complete helper
     * string, not a named method's return value.
     *
     * @param args the suffixes the text has to end with, at least one of which has to match for a
     *             candidate to be kept
     * @return this builder, for chaining
     * @throws IllegalStateException if no start step is waiting to be completed
     * @since 1.6.5
     */
    public WorldScannerBuilder endsWith(String... args) {
        createStringFilter("ENDS_WITH", args);
        return this;
    }

    /**
     * Completes a pending string filter, keeping a candidate when its complete helper text matches
     * any supplied regular expression. Patterns are alternatives; matching is against the whole
     * text rather than a substring search. Use {@link #contains(String[])} for literal substrings.
     *
     * @param args the regular expressions the whole text has to match, at least one of which has to
     *             match for a candidate to be kept
     * @return this builder, for chaining
     * @throws IllegalStateException if no start step is waiting to be completed
     * @since 1.6.5
     */
    public WorldScannerBuilder matches(String... args) {
        createStringFilter("MATCHES", args);
        return this;
    }

    @SuppressWarnings("unchecked")
    private void createStringFilter(String method, String... args) {
        if (selectedCategory == FilterCategory.STATE) {
            composeFilters(new StringifyFilter<BlockStateHelper>(method).addOption(args));
        } else if (selectedCategory == FilterCategory.BLOCK) {
            composeFilters(new StringifyFilter<BlockHelper>(method).addOption(args));
        } else {
            throw new IllegalStateException("Can't create filter, because there is none.");
        }
    }

    /**
     * Builds a scanner using the completed filters and the client's current level. An unset filter
     * category is unfiltered; the builder's filters are retained, so it can build multiple scanners.
     * <pre>
     * // a block filter and a state filter are two independent stages, so a
     * // block that is hard enough and a state that needs no tool is one set
     * // of blocks rather than two alternative ones
     * const scanner = World.getWorldScanner()
     *   .withBlockFilter("getHardness").is(">=", 10)
     *   .withStateFilter("isToolRequired").is(false)
     *   .build();
     * const found = scanner.scanAroundPlayer(2);
     * Chat.log(`${found.size()} hard blocks that break without a tool`);
     * </pre>
     *
     * @return a scanner over the client's current level
     * @since 1.6.5
     */
    public WorldScanner build() {
        return new WorldScanner(Minecraft.getInstance().level, blockFilter, stateFilter, sequential);
    }

    private enum Operation {
        NEW,
        OR,
        AND,
        NOT,
        XOR,
        NONE
    }

    private enum FilterCategory {
        BLOCK,
        STATE,
        NONE
    }

}
