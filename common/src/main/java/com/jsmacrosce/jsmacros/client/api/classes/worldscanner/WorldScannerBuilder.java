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
 * The builder can be used to create a world scanner with native java functions. This is especially useful for languages like javascript that
 * don't support multithreading, which causes streams to run sequential instead of parallel.
 * The builder has two filters for the block and the block state, which need to be configured separately.
 * If one function is not defined, it will just be ignored when building the scanner.<br>
 * The block and block state filters have to start with a 'with' command like {@link #withStateFilter(String)} or {@link #withStringBlockFilter()}.
 * A second, complete 'with' step for the same category overwrites the first rather than combining with it; to add to a filter that is already there,
 * use a command with the prefix 'and' or 'or' instead. The 'not' command negates the whole block or block state filter and doesn't need any arguments.
 * The 'xor' steps accept a block or state when exactly one of the combined filters accepts it.<br>
 * <p>
 * Every step that starts a filter has to be <i>completed</i> before the next one starts, and it is the completing call that builds anything at all.
 * A start step only records what the next call should build, so calling a second start step first is refused rather than allowed:
 * {@link #withBlockFilter(String)} and then {@link #andBlockFilter(String)} throws
 * {@code IllegalStateException("Can't create a new filter, because the old one is not completed.")} at the second of the two.
 * The calls that complete a step are {@link #is(Object[])}, {@link #test(Object[])} and the five string functions
 * {@link #equals(String[])}, {@link #contains(String[])}, {@link #startsWith(String[])}, {@link #endsWith(String[])} and
 * {@link #matches(String[])}. The two 'not' steps are the exception: they complete themselves inside their own call and so take nothing after them.<br>
 * <p>
 * All other commands need some arguments to work. For String functions, it's one of these functions: 'equals', 'contains', 'startsWith', 'endsWith' or 'matches'.
 * The strings to match are passed as vararg parameters (as many as needed, separated by a comma, {@code is("chest", "barrel", "ore")}) and the filter acts
 * like a logical or, so only one of the arguments needs to match the criteria. It should be noted, that string functions call the toString method, so
 * comparing a block with something like "minecraft:stone" will always return false, because the toString method of the block helper gives
 * {@code BlockHelper:{"id": "minecraft:stone"}} and of the block state helper gives
 * {@code BlockStateHelper:{"id": "minecraft:stone", "properties": {...}}}. A pattern therefore has to be written against that whole wrapper:
 * {@link #matches(String[])} takes any string, so a substring is enough there, while {@link #equals(String[])} and the other three take the
 * complete text. The other way to get at a bare id is a step that names a method, as in {@link #withBlockFilter(String)} with {@code "getId"},
 * which compares the id on its own rather than the wrapper around it.<br>
 * This matches any block whose text includes '_ore', plus any block whose text is exactly the one chest id:
 * <pre>
 * // matches takes a pattern, and matches is a whole string match, so the
 * // leading and trailing .* are what let it find the id inside the wrapper
 * const ores = World.getWorldScanner()
 *   .withStringBlockFilter().matches(".*_ore.*")
 *   .orStringBlockFilter().matches(".*_block.*")
 *   .build();
 * Chat.log(`${ores.scanAroundPlayer(2).size()} ore or building blocks`);
 * </pre>
 * <p>
 * For non String functions, the method name must be passed when creating the filter. The names can be any method in {@link BlockStateHelper} or {@link BlockHelper}.
 * More precisely, they are public no-argument methods, including inherited methods but excluding methods declared by Object. An unknown name
 * is refused with a {@code NullPointerException} with {@code Unknown filter method: <name>}. A name whose method returns something other than a number, a
 * String or a boolean is refused too, with an {@code IllegalArgumentException}: on the block helper that rules out {@code getName}, {@code getDefaultState},
 * {@code getStates}, {@code getTags} and {@code getDefaultItemStack}, and on the block state helper it rules out {@code getBlock}, {@code getFluidState} and
 * {@code getUniversal}. Both helpers override {@code toString}, so that is a name like any other and behaves like {@link #withStringBlockFilter()} does.
 * For more complex filters, use the MethodWrapper function {@link FWorld#getWorldScanner(MethodWrapper, MethodWrapper)}.
 * The overloads taking an {@link IFilter} install completed callback filters and force scanners built by this builder to run sequentially.
 * Depending on the return type of the method, the following parameters must be passed to 'is' or 'test'. There are two methods, because 'is' is a keyword in some languages.<br>
 * <pre>
 * For any number:
 *   - is(operation, number) with operation = {@code >}, {@code >=}, {@code <}, {@code <=},
 *     {@code ==} or {@code !=} and the number that should be compared to,
 *     i.e. is(">=", 8) returns true if the returned number is greater or equal to 8.
 *     Both arguments are required: a single number is a ClassCastException, because the first one is read as the operation.
 *     The operation itself is not checked here. A misspelled one builds fine and only fails when a block is actually tested,
 *     so a filter that throws IllegalArgumentException during a scan rather than at build() means the operation is the problem.
 * For any String:
 *   - is(method, string) with method = 'EQUALS', 'CONTAINS', 'STARTS_WITH', 'ENDS_WITH', 'MATCHES' and the string is the one to compare the returned value to,
 *     i.e. is("ENDS_WITH", "ore") checks if the returned string ends with ore (can be used with withBlockFilter("getId")).
 *     Unlike a number operation, this one is checked at build time, so a misspelled method name throws IllegalArgumentException from the is call itself.
 * For any Boolean:
 *   - is(val) with val either {@code true} or {@code false}
 *     i.e. is(false) returns true if the returned boolean value is false
 * </pre>
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
     * makes a builder with no filters on either category.
     * <p>
     * Both categories start empty, so a scanner built straight from this keeps every state. There
     * is nothing else to configure before the first step, and nothing to undo, so this is the
     * whole of the starting state rather than something a first filter step depends on.
     * <p>
     * A builder is normally obtained from {@code World.getWorldScanner()}, which is the typed
     * route and the one to reach for. This constructor is on the class rather than on the library
     * for the sake of the two the typed route leaves off, and a script wanting those goes through
     * the raw class as their examples show.
     * example:
     * <pre>
     * // a builder with nothing on it keeps every block, so a scan of it
     * // is a plain walk of the region
     * const Builder = Java.type("com.jsmacrosce.jsmacros.client.api.classes.worldscanner.WorldScannerBuilder");
     * const scanner = new Builder().build();
     * Chat.log(`${scanner.scanAroundPlayer(0).size()} blocks in the player's chunk`);
     * </pre>
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
     * starts the block state filter over a named method of the block state helper.
     * <p>
     * Nothing is built by this call. It records that the next completing step should build a state
     * filter on the given method, and it is that later call which is where a bad method name, a bad
     * comparison name or a missing argument is refused. If a start step for either category is already
     * waiting to be completed, this throws rather than starting a second one.
     * <p>
     * A first, complete 'with' step for the state category replaces whatever the state slot held
     * before, including the negation a {@link #notStateFilter()} may have left there.
     * example:
     * <pre>
     * // isToolRequired is a declared public method of the state helper taking
     * // no parameters and returning a boolean, so is takes only the value to
     * // match. The with step is the one that names the method
     * const scanner = World.getWorldScanner()
     *   .withStateFilter("isToolRequired").is(false)
     *   .build();
     * const found = scanner.scanAroundPlayer(2);
     * Chat.log(`${found.size()} states that break without a tool`);
     * </pre>
     *
     * @param method the name of a public no-argument method available on
     *               {@link BlockStateHelper}, and the arguments the following
     *               {@link #is(Object[], Object[])} needs are decided by its return type
     * @return this builder, for chaining
     * @throws IllegalStateException if a start step is already waiting to be completed, which
     *         is what a second 'with', 'and' or 'or' step in a row does
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
     * adds a block state filter on a named method to the one already there, keeping a state only
     * when both accept it.
     * <p>
     * Like {@link #withStateFilter(String)} this builds nothing on its own; the following
     * {@link #is(Object[], Object[])} or string function is where the filter is created and joined.
     * Joining needs a state filter to already be in the slot, so this throws if the state filter has
     * not been started, and it throws too if a start step for either category is already waiting.
     * example:
     * <pre>
     * // the second step has to name a method of its own and be given its own
     * // is. getLuminance returns an int, so this one takes an operator and a
     * // number rather than the single value a boolean method would take
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
     * @throws IllegalStateException if the state filter has not been started yet, or if a start
     *         step is already waiting to be completed
     * @since 1.6.5
     */
    public WorldScannerBuilder andStateFilter(String method) {
        createNewFilter(Operation.AND, FilterCategory.STATE, method);
        return this;
    }

    /**
     * adds a block state filter on a named method to the one already there, keeping a state when
     * either filter accepts it.
     * <p>
     * Like {@link #withStateFilter(String)} this builds nothing on its own; the following
     * {@link #is(Object[], Object[])} or string function is where the filter is created and joined.
     * Joining needs a state filter to already be in the slot, so this throws if the state filter has
     * not been started, and it throws too if a start step for either category is already waiting.
     * example:
     * <pre>
     * // a stone slab is solid and a torch is not, so this keeps one or the
     * // other. getId returns a String, so the arguments are the name of a
     * // comparison and the text to compare the id against
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
     * @throws IllegalStateException if the state filter has not been started yet, or if a start
     *         step is already waiting to be completed
     * @since 1.6.5
     */
    public WorldScannerBuilder orStateFilter(String method) {
        createNewFilter(Operation.OR, FilterCategory.STATE, method);
        return this;
    }

    /**
     * Starts a block-state comparison combined with the existing state filter using XOR.
     * Complete it with a comparison call; exactly one constituent filter must accept a state.
     * If no state filter exists, completing the comparison throws {@code IllegalStateException}.
     *
     * @param method a public no-argument method, including inherited methods
     * @return this builder for chaining
     * @throws IllegalStateException if a filter step is pending
     */
    public WorldScannerBuilder xorStateFilter(String method) {
        createNewFilter(Operation.XOR, FilterCategory.STATE, method);
        return this;
    }

    /**
     * negates the whole block state filter, keeping exactly the states it rejected.
     * <p>
     * This is the one start step that completes itself, so nothing follows it and it takes no method
     * name: the negation is applied to whatever the state slot already holds. Because it is applied
     * immediately rather than left pending, it throws from inside this call, not later at
     * {@link #build()}, and it throws when the state filter has not been started, since there is
     * nothing to negate.
     * example:
     * <pre>
     * // everything that is not a full cube. The negation applies to
     * // the filter that was already in the slot, not to a method
     * // named here, and there is no name to give
     * const scanner = World.getWorldScanner()
     *   .withStateFilter("exceedsCube").is(false)
     *   .notStateFilter()
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} states that are not full cubes`);
     * </pre>
     *
     * @return this builder, for chaining
     * @throws IllegalStateException if the state filter has not been started, which is the
     *         {@code "Can't compose null filters."} case, or if a start step is already waiting to
     *         be completed
     * @since 1.6.5
     */
    public WorldScannerBuilder notStateFilter() {
        createNewFilter(Operation.NOT, FilterCategory.STATE, "");
        composeFilters(null);
        return this;
    }

    /**
     * starts the block filter over a named method of the block helper.
     * <p>
     * Nothing is built by this call. It records that the next completing step should build a block
     * filter on the given method, and it is that later call which is where a bad method name, a bad
     * comparison name or a missing argument is refused. If a start step for either category is already
     * waiting to be completed, this throws rather than starting a second one.
     * <p>
     * A block filter and a state filter are two independent stages rather than alternatives, so a
     * state filter set afterwards narrows the block filter rather than replacing it.
     * example:
     * <pre>
     * // getHardness is a declared public method of the block helper taking no
     * // parameters and returning a float, so is takes an operator and a
     * // number. The with step is the one that names the method
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
     * @throws IllegalStateException if a start step is already waiting to be completed, which
     *         is what a second 'with', 'and' or 'or' step in a row does
     * @since 1.6.5
     */
    public WorldScannerBuilder withBlockFilter(String method) {
        createNewFilter(Operation.NEW, FilterCategory.BLOCK, method);
        return this;
    }

    /**
     * Installs a completed callback filter for blocks and forces sequential scanning.
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
     * adds a block filter on a named method to the one already there, keeping a block only when
     * both accept it.
     * <p>
     * Like {@link #withBlockFilter(String)} this builds nothing on its own; the following
     * {@link #is(Object[], Object[])} or string function is where the filter is created and joined.
     * Joining needs a block filter to already be in the slot, so this throws if the block filter has
     * not been started, and it throws too if a start step for either category is already waiting.
     * example:
     * <pre>
     * // a second block filter has to name a method of its own and be given
     * // its own is. Both here are numbers, so both take an operator and a
     * // number, and the two have to agree for a block to be kept
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
     * adds a block filter on a named method to the one already there, keeping a block when either
     * filter accepts it.
     * <p>
     * Like {@link #withBlockFilter(String)} this builds nothing on its own; the following
     * {@link #is(Object[], Object[])} or string function is where the filter is created and joined.
     * Joining needs a block filter to already be in the slot, so this throws if the block filter has
     * not been started, and it throws too if a start step for either category is already waiting.
     * example:
     * <pre>
     * // getJumpVelocityMultiplier returns a float and getId returns a
     * // String, so the two steps take different arguments even though they
     * // are joined the same way
     * const scanner = World.getWorldScanner()
     *   .withBlockFilter("getJumpVelocityMultiplier").is(">", 0.7)
     *   .orBlockFilter("getId").is("ENDS_WITH", "sponge")
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} jumpy blocks or sponges`);
     * </pre>
     *
     * @param method the name of a public no-argument method available on {@link BlockHelper}
     * @return this builder, for chaining
     * @throws IllegalStateException if the block filter has not been started yet, or if a start
     *         step is already waiting to be completed
     * @since 1.6.5
     */
    public WorldScannerBuilder orBlockFilter(String method) {
        createNewFilter(Operation.OR, FilterCategory.BLOCK, method);
        return this;
    }

    /**
     * Starts a block comparison combined with the existing block filter using XOR.
     * Complete it with a comparison call; exactly one constituent filter must accept a block.
     * If no block filter exists, completing the comparison throws {@code IllegalStateException}.
     *
     * @param method a public no-argument method, including inherited methods
     * @return this builder for chaining
     * @throws IllegalStateException if a filter step is pending
     */
    public WorldScannerBuilder xorBlockFilter(String method) {
        createNewFilter(Operation.XOR, FilterCategory.BLOCK, method);
        return this;
    }

    /**
     * negates the whole block filter, keeping exactly the blocks it rejected.
     * <p>
     * This is the one start step that completes itself, so nothing follows it and it takes no method
     * name: the negation is applied to whatever the block slot already holds. Because it is applied
     * immediately rather than left pending, it throws from inside this call, not later at
     * {@link #build()}, and it throws when the block filter has not been started, since there is
     * nothing to negate.
     * example:
     * <pre>
     * // everything that is not exactly one block id, by negating the filter
     * // that was there. Negation is of the whole slot, not of the last step
     * const scanner = World.getWorldScanner()
     *   .withBlockFilter("getId").is("EQUALS", "minecraft:chest")
     *   .notBlockFilter()
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} blocks that are not chests`);
     * </pre>
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
     * starts a block filter that matches on the block helper's own text rather than on a method's
     * return value.
     * <p>
     * This takes no method name because there is nothing to name: the filter asks each block helper
     * for its string form, which is {@code BlockHelper:{"id": "minecraft:stone"}}, and compares that.
     * A step naming a method whose return value is the string would match the same text, so
     * {@code withBlockFilter("toString")} and this are two routes to one filter.
     * <p>
     * Nothing is built until one of {@link #equals(String[])}, {@link #contains(String[])},
     * {@link #startsWith(String[])}, {@link #endsWith(String[])} or {@link #matches(String[])}
     * follows it.
     * example:
     * <pre>
     * // matches is a whole string match, so the .* at each end is what lets
     * // it find the id inside the wrapper. A bare "stone" would not match,
     * // because the text is the whole wrapper rather than the id in it
     * const scanner = World.getWorldScanner()
     *   .withStringBlockFilter().matches(".*minecraft:chest.*")
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} chests`);
     * </pre>
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
     * adds a block filter on the block helper's own text to the one already there, keeping a block
     * only when both accept it.
     * <p>
     * Nothing is built until one of the five string functions follows it, and joining needs a block
     * filter to already be in the slot, so this throws if the block filter has not been started.
     * example:
     * <pre>
     * // both steps are on the same text, and the block has to match both
     * // patterns for the 'and' to keep it
     * const scanner = World.getWorldScanner()
     *   .withStringBlockFilter().matches(".*_log.*")
     *   .andStringBlockFilter().matches(".*minecraft:oak.*")
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} oak logs`);
     * </pre>
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
     * adds a block filter on the block helper's own text to the one already there, keeping a block
     * when either filter accepts it.
     * <p>
     * Nothing is built until one of the five string functions follows it, and joining needs a block
     * filter to already be in the slot, so this throws if the block filter has not been started.
     * example:
     * <pre>
     * // the two patterns are alternatives, so this keeps a block matching
     * // either of them rather than both
     * const scanner = World.getWorldScanner()
     *   .withStringBlockFilter().matches(".*_ore.*")
     *   .orStringBlockFilter().matches(".*chest.*")
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} ores or chests`);
     * </pre>
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
     * Starts a string block filter combined with the existing block filter using XOR.
     * Complete it with a string comparison; exactly one constituent filter must accept.
     * If no block filter exists, completing the comparison throws {@code IllegalStateException}.
     *
     * @return this builder for chaining
     * @throws IllegalStateException if a filter step is pending
     */
    public WorldScannerBuilder xorStringBlockFilter() {
        createNewFilter(Operation.XOR, FilterCategory.BLOCK, "");
        return this;
    }

    /**
     * starts a block state filter that matches on the block state helper's own text rather than on
     * a method's return value.
     * <p>
     * This takes no method name because there is nothing to name: the filter asks each block state
     * helper for its string form, which is the id together with the property map, such as
     * {@code BlockStateHelper:{"id": "minecraft:oak_stairs", "properties": {facing=north, half=bottom}}},
     * and compares that. Because the properties are part of the text, a state can be matched on one
     * of them here, which a method step cannot do without naming a method that returns the value.
     * <p>
     * Nothing is built until one of {@link #equals(String[])}, {@link #contains(String[])},
     * {@link #startsWith(String[])}, {@link #endsWith(String[])} or {@link #matches(String[])}
     * follows it.
     * example:
     * <pre>
     * // matches is a whole string match, so the leading .* is what lets the
     * // pattern reach the property map past the id
     * const scanner = World.getWorldScanner()
     *   .withStringStateFilter().matches(".*facing=north.*")
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} states facing north`);
     * </pre>
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
     * adds a block state filter on the block state helper's own text to the one already there,
     * keeping a state only when both accept it.
     * <p>
     * Nothing is built until one of the five string functions follows it, and joining needs a state
     * filter to already be in the slot, so this throws if the state filter has not been started.
     * example:
     * <pre>
     * // a state has to match both patterns for the 'and' to keep it
     * const scanner = World.getWorldScanner()
     *   .withStringStateFilter().matches(".*facing=north.*")
     *   .andStringStateFilter().matches(".*half=bottom.*")
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} lower north facing states`);
     * </pre>
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
     * adds a block state filter on the block state helper's own text to the one already there,
     * keeping a state when either filter accepts it.
     * <p>
     * Nothing is built until one of the five string functions follows it, and joining needs a state
     * filter to already be in the slot, so this throws if the state filter has not been started.
     * example:
     * <pre>
     * // a block state filter and a block state string filter are two
     * // independent things, so the one started here narrows whatever the
     * // earlier state filter kept
     * const scanner = World.getWorldScanner()
     *   .withStateFilter("isToolRequired").is(false)
     *   .orStringStateFilter().matches(".*facing=north.*")
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} states breaking without a tool or facing north`);
     * </pre>
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
     * Starts a string state filter combined with the existing state filter using XOR.
     * Complete it with a string comparison; exactly one constituent filter must accept.
     * If no state filter exists, completing the comparison throws {@code IllegalStateException}.
     *
     * @return this builder for chaining
     * @throws IllegalStateException if a filter step is pending
     */
    public WorldScannerBuilder xorStringStateFilter() {
        createNewFilter(Operation.XOR, FilterCategory.STATE, "");
        return this;
    }

    /**
     * completes the pending filter step with the arguments the comparison needs, and joins it to the
     * filter already there.
     * <p>
     * This is the form where the named method takes no arguments, which is every method either
     * helper declares as usable, so the whole of {@code args} is the comparison's own arguments. They
     * are decided by the return type of the named method: a number takes an operator and a number, a
     * String takes the name of a comparison and the text, a boolean takes the single value to match.
     * A numeric method needs both of its arguments, since the first is read as the operator and a
     * single number is a {@code ClassCastException}. A numeric operator is not checked here: a
     * misspelled one builds fine and only fails when a block is actually tested.
     * <p>
     * The other {@link #is(Object[], Object[])} takes a second array for when the named method takes
     * arguments, which none of the helpers' own declared methods do, so this is the one a script uses.
     * example:
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
     * @throws NullPointerException if the named method is not a public no-argument method declared
     *         by the helper for the pending category
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
     * completes the pending filter step, passing one argument list to the named method and another to
     * the comparison, and joins it to the filter already there.
     * <p>
     * Neither helper declares a public no-argument method that takes parameters, so a method name
     * with arguments to pass cannot be named here, and this is a route a script has no use for. The
     * one-array {@link #is(Object[])} is the one to reach for, and it is what that method calls with
     * a {@code null} first argument. It is also left off the typed builder that
     * {@code World.getWorldScanner()} returns, so the only way to reach it is the raw class.
     * example:
     * <pre>
     * // this overload is here for completeness. Nothing the helpers declare
     * // takes arguments, so the two-array form has nothing to name. It is
     * // not on the typed builder World.getWorldScanner() returns, so the raw
     * // class is what reaches it
     * const Builder = Java.type("com.jsmacrosce.jsmacros.client.api.classes.worldscanner.WorldScannerBuilder");
     * const scanner = new Builder()
     *   .withBlockFilter("getHardness").is(null, [">=", 10])
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} blocks of hardness 10 or more`);
     * </pre>
     *
     * @param methodArgs the arguments to call the named method with, or {@code null} for none
     * @param filterArgs the arguments for the comparison, whose count and types are decided by the
     *                   return type of the named method
     * @return this builder, for chaining
     * @throws IllegalStateException if no start step is waiting to be completed
     * @throws NullPointerException if the named method is not a public no-argument method declared
     *         by the helper for the pending category
     * @throws IllegalArgumentException if the named method returns a type the comparison cannot
     *         handle, or, for a String method, if the comparison name is not one of the five
     * @throws ClassCastException if the first argument is not a string, which is what happens to a
     *         numeric method given a single number
     * @throws ArrayIndexOutOfBoundsException if there are fewer arguments than the return type
     *         needs, which is none at all for a boolean method and one for a numeric or String one
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
     * an alias of {@link #is(Object[])}, for a language in which {@code is} is a keyword.
     * <p>
     * It is the same call with the same arguments and the same exceptions; only the name differs.
     * The alias is left off the typed builder that {@code World.getWorldScanner()} returns, so a
     * script that wants it goes through the raw class.
     * example:
     * <pre>
     * // test is is under another name. getHardness returns a float, so the
     * // arguments are still an operator and a number. The typed builder
     * // World.getWorldScanner() returns does not carry test, so this is
     * // reached through the raw class
     * const Builder = Java.type("com.jsmacrosce.jsmacros.client.api.classes.worldscanner.WorldScannerBuilder");
     * const scanner = new Builder()
     *   .withBlockFilter("getHardness").test(">=", 10)
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} blocks of hardness 10 or more`);
     * </pre>
     *
     * @param args the arguments for the comparison, whose count and types are decided by the
     *             return type of the method the pending step named
     * @return this builder, for chaining
     * @throws IllegalStateException if no start step is waiting to be completed
     * @throws NullPointerException if the named method is not a public no-argument method declared
     *         by the helper for the pending category
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
     * an alias of {@link #is(Object[], Object[])}, for a language in which {@code is} is a keyword.
     * <p>
     * It is the same call with the same arguments and the same exceptions; only the name differs.
     * Nothing the helpers declare takes arguments, so the two-array form has no use from a script,
     * and like {@link #test(Object[])} it is left off the typed builder.
     * example:
     * <pre>
     * // the two-array form of test, which is the same thing is does with
     * // a null first argument. The typed builder carries neither, so this
     * // is reached through the raw class
     * const Builder = Java.type("com.jsmacrosce.jsmacros.client.api.classes.worldscanner.WorldScannerBuilder");
     * const scanner = new Builder()
     *   .withBlockFilter("getHardness").test(null, [">=", 10])
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} blocks of hardness 10 or more`);
     * </pre>
     *
     * @param methodArgs the arguments to call the named method with, or {@code null} for none
     * @param filterArgs the arguments for the comparison, whose count and types are decided by the
     *                   return type of the named method
     * @return this builder, for chaining
     * @throws IllegalStateException if no start step is waiting to be completed
     * @throws NullPointerException if the named method is not a public no-argument method declared
     *         by the helper for the pending category
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
     * completes the pending string step by keeping a candidate only when its text is exactly one of
     * the patterns given.
     * <p>
     * This completes a filter started by one of the 'withString' or 'andString' or 'orString' steps,
     * and a block or state filter over a named method is not involved. The patterns are alternatives
     * rather than a conjunction, so a candidate is kept when any one of them is equal, and a step
     * written with no patterns at all keeps nothing. The text is the helper's own string form, so
     * {@code equals("minecraft:chest")} matches nothing; the whole wrapper
     * {@code BlockHelper:{"id": "minecraft:chest"}} is what an exact match needs, and
     * {@link #matches(String[])} is the one to reach for when a substring is wanted.
     * example:
     * <pre>
     * // equals is an exact match, so the pattern has to be the whole
     * // wrapper the block helper stringifies to, not the bare id
     * const scanner = World.getWorldScanner()
     *   .withStringBlockFilter().equals('BlockHelper:{"id": "minecraft:chest"}')
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} chests`);
     * </pre>
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
     * completes the pending string step by keeping a candidate when its text includes any one of the
     * patterns given.
     * <p>
     * This completes a filter started by one of the 'withString' or 'andString' or 'orString' steps,
     * and a block or state filter over a named method is not involved. The patterns are alternatives
     * rather than a conjunction, so a candidate is kept as soon as one of them is found in the text.
     * The text is the helper's own string form, so a pattern such as {@code "minecraft:chest"} is
     * found inside {@code BlockHelper:{"id": "minecraft:chest"}} and matches; the pattern is a
     * literal substring and not a regular expression, so characters that mean something in one are
     * taken here as themselves, and {@link #matches(String[])} is the one that reads a pattern.
     * <p>
     * The typed builder that {@code World.getWorldScanner()} returns narrows this call to the
     * complete wrapper, so on that surface a substring is rejected and only the whole text is
     * accepted, which is what {@link #equals(String[])} already does. The raw class takes any
     * string, and the example below goes through it for that reason.
     * example:
     * <pre>
     * // contains is a plain substring search over the whole wrapper, so
     * // "_ore" finds every ore block without spelling out each id. The
     * // typed builder only accepts the complete wrapper here, so the raw
     * // class is what takes a substring
     * const Builder = Java.type("com.jsmacrosce.jsmacros.client.api.classes.worldscanner.WorldScannerBuilder");
     * const scanner = new Builder()
     *   .withStringBlockFilter().contains("_ore")
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} ore blocks`);
     * </pre>
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
     * completes the pending string step by keeping a candidate when its text begins with any one of
     * the patterns given.
     * <p>
     * This completes a filter started by one of the 'withString' or 'andString' or 'orString' steps,
     * and a block or state filter over a named method is not involved. The patterns are alternatives
     * rather than a conjunction. Because the text of a block helper always begins with
     * {@code BlockHelper:} whatever the block is, a pattern of {@code "BlockHelper"} keeps every
     * block and any other pattern keeps none: the useful prefixes are the wrapper's fixed opening
     * rather than anything about the block.
     * example:
     * <pre>
     * // every block helper begins with the same text, so this keeps all of
     * // them. It is here to show the shape of the call; a state filter is
     * // where startsWith has a block specific prefix to work with
     * const scanner = World.getWorldScanner()
     *   .withStringBlockFilter().startsWith("BlockHelper")
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} blocks`);
     * </pre>
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
     * completes the pending string step by keeping a candidate when its text ends with any one of the
     * patterns given.
     * <p>
     * This completes a filter started by one of the 'withString' or 'andString' or 'orString' steps,
     * and a block or state filter over a named method is not involved. The patterns are alternatives
     * rather than a conjunction. The end of the text is the last thing the wrapper closes with, so a
     * block helper's text ends with {@code "}}, and a state helper's text does too, which makes this
     * a filter on nothing in particular. To test the end of an id rather than the end of the wrapper,
     * name {@code getId} and use {@link #is(Object[], Object[])} with the {@code ENDS_WITH}
     * comparison, which reads the id on its own.
     * example:
     * <pre>
     * // ends with a fixed closing brace, so this keeps every state. To get
     * // at the end of an id instead, the method step below compares the id
     * // itself, which is the only place an id appears without the wrapper
     * const scanner = World.getWorldScanner()
     *   .withStringBlockFilter().endsWith("}")
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} blocks`);
     * </pre>
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
     * completes the pending string step by keeping a candidate when its text matches any one of the
     * patterns given.
     * <p>
     * This completes a filter started by one of the 'withString' or 'andString' or 'orString' steps,
     * and a block or state filter over a named method is not involved. The patterns are alternatives
     * rather than a conjunction. This is the one string function that reads its patterns as regular
     * expressions rather than as literal text, and it is a whole text match rather than a search, so
     * a pattern has to allow for the wrapper around whatever it is looking for. It is therefore the
     * one that finds an id or a property inside the wrapper, where
     * {@link #contains(String[])} would do as well without the anchors.
     * example:
     * <pre>
     * // matches reads the pattern as a regular expression and matches the
     * // whole text, so the leading and trailing .* are what let it reach
     * // the id inside the wrapper
     * const scanner = World.getWorldScanner()
     *   .withStringBlockFilter().matches(".*minecraft:diamond_ore.*")
     *   .build();
     * Chat.log(`${scanner.scanAroundPlayer(2).size()} diamond ore`);
     * </pre>
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
     * builds the scanner from whatever filters have been completed so far.
     * <p>
     * The scanner is built over the client's current level, so with no world loaded that level is
     * null and every scan that reaches the world is a problem rather than an answer. A category left
     * without a filter is passed as null, which is not an error: the scanner then keeps every state
     * for that stage. This reads the filters and moves on, it does not clear them, so the same
     * builder can be built from more than once and a scan started from the first is unaffected by
     * what a later one adds.
     * <p>
     * Building is not the point at which a bad filter is found. A numeric operator that was
     * misspelled survives the build and only fails while a scan is walking blocks, and a string
     * comparison name is checked at the step that named it, well before here.
     * example:
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
     * @return a scanner over the client's current level, with a null filter for each category that
     *         was never started
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
