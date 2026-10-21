package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.*;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.JsMacrosClient;
import com.jsmacrosce.jsmacros.client.api.helper.CommandContextHelper;
import com.jsmacrosce.jsmacros.client.api.helper.SuggestionsBuilderHelper;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.core.EventLockWatchdog;
import com.jsmacrosce.jsmacros.core.MethodWrapper;
import com.jsmacrosce.jsmacros.core.classes.Registrable;
import com.jsmacrosce.jsmacros.core.config.CoreConfigV2;
import com.jsmacrosce.jsmacros.core.event.BaseEvent;
import com.jsmacrosce.jsmacros.core.event.IEventListener;
import com.jsmacrosce.jsmacros.core.language.EventContainer;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Builds a command tree for client registration; obtain it from
 * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FChat#getCommandManager()}.
 * Each argument extends the current branch; use {@link #or()} or {@link #or(int)} to move
 * back up the tree before adding an alternative branch.
 * <p>
 * {@link #executes(MethodWrapper)} attaches to the current node. Argument values are read from
 * the callback's {@link com.jsmacrosce.jsmacros.client.api.helper.CommandContextHelper} with
 * {@code getArg}; their value types depend on the argument methods used.
 * <p>
 * Register the completed tree with {@link #register()}.
 * <pre>
 * const command = Chat.getCommandManager().createCommandBuilder("count");
 * // /count set 10
 * command.literalArg("set")
 * .intArg("amount", 1, 64)
 * .suggestMatching("10", "32", "64")
 * .executes(JavaWrapper.methodToJava(function (ctx) {
 *   Chat.log(`the amount is now ${ctx.getArg("amount")}`);
 *   return true;
 * }));
 * // fold the whole branch back up to the command name, so the next literal
 * // lands alongside "set" rather than below it
 * command.or(1);
 * // /count show
 * command.literalArg("show")
 * .executes(JavaWrapper.methodToJava(function (ctx) {
 *   Chat.log("the amount is whatever it was");
 *   return true;
 * }));
 * command.register();
 * </pre>
 *
 * @since 1.4.2
 */
@DocletCategory("Commands")
@SuppressWarnings("unused")
public abstract class CommandBuilder implements Registrable<CommandBuilder> {

    protected abstract void argument(String name, Supplier<ArgumentType<?>> type);

    protected abstract void argument(String name, Function<CommandBuildContext, ArgumentType<?>> type);

    /**
     * Adds a fixed word to the current command branch; it has no value to read from the context.
     * @param name the word the player types
     * @return self for chaining.
     */
    public abstract CommandBuilder literalArg(String name);

    /**
     * Adds a boolean argument accepting {@code true} or {@code false}; its parsed value is a boolean.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder booleanArg(String name) {
        argument(name, BoolArgumentType::bool);
        return this;
    }

    /**
     * Adds an unbounded signed 32-bit integer argument.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder intArg(String name) {
        argument(name, (Supplier<ArgumentType<?>>) IntegerArgumentType::integer);
        return this;
    }

    /**
     * Adds a signed 32-bit integer argument constrained to the inclusive range {@code min}–{@code max}.
     * @param name the name the argument is read back under
     * @param min  the lowest value accepted, inclusive
     * @param max  the highest value accepted, inclusive
     * @return self for chaining.
     */
    public CommandBuilder intArg(String name, int min, int max) {
        argument(name, () -> IntegerArgumentType.integer(min, max));
        return this;
    }

    /**
     * Adds an integer argument accepting either one value or a {@code min..max} range; the parsed value is a Brigadier range object.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder intRangeArg(String name) {
        argument(name, RangeArgument::intRange);
        return this;
    }

    /**
     * Adds an unbounded signed 64-bit integer argument.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder longArg(String name) {
        argument(name, (Supplier<ArgumentType<?>>) LongArgumentType::longArg);
        return this;
    }

    /**
     * Adds a signed 64-bit integer argument constrained to the inclusive range {@code min}–{@code max}.
     * @param name the name the argument is read back under
     * @param min  the lowest value accepted, inclusive
     * @param max  the highest value accepted, inclusive
     * @return self for chaining.
     */
    public CommandBuilder longArg(String name, long min, long max) {
        argument(name, () -> LongArgumentType.longArg(min, max));
        return this;
    }

    /**
     * Adds a decimal argument accepting either one value or a {@code min..max} range; the parsed value is a Brigadier range object.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder floatRangeArg(String name) {
        argument(name, RangeArgument::floatRange);
        return this;
    }

    /**
     * Adds an unbounded decimal-number argument.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder doubleArg(String name) {
        argument(name, (Supplier<ArgumentType<?>>) DoubleArgumentType::doubleArg);
        return this;
    }

    /**
     * Adds a bounded decimal argument. Both bounds are inclusive; values outside them are
     * rejected during parsing. This is the decimal counterpart to
     * {@link #intArg(String, int, int)}.
     * @param name the name the argument is read back under
     * @param min  the lowest value accepted, inclusive
     * @param max  the highest value accepted, inclusive
     * @return self for chaining.
     */
    public CommandBuilder doubleArg(String name, double min, double max) {
        argument(name, () -> DoubleArgumentType.doubleArg(min, max));
        return this;
    }

    /**
     * Adds a UUID argument; {@link com.jsmacrosce.jsmacros.client.api.helper.CommandContextHelper#getArg(String)}
     * returns its parsed value as a {@link java.util.UUID}.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder uuidArgType(String name) {
        argument(name, UuidArgument::uuid);
        return this;
    }

    /**
     * Adds a greedy text argument that consumes the remaining input, including spaces. It must
     * be the final argument on its branch.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder greedyStringArg(String name) {
        argument(name, StringArgumentType::greedyString);
        return this;
    }

    /**
     * Adds a quoted string argument, which can contain spaces and ends at the closing quote.
     * The parsed value is returned without its quotes.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder quotedStringArg(String name) {
        argument(name, StringArgumentType::string);
        return this;
    }

    /**
     * Adds a single-word string argument, which does not accept spaces.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder wordArg(String name) {
        argument(name, StringArgumentType::word);
        return this;
    }

    /**
     * Adds an argument matched against a regular expression and returns an array containing the
     * whole match followed by its capture groups.
     * <p>
     * The pattern must match at the start of the remaining input; the entire match is consumed.
     * <p>
     * A failed match reports the pattern to the player between slashes.
     * <p>
     * Recognized flag characters are:
     * <ul>
     * <li>{@code i} makes the match ignore case</li>
     * <li>{@code s} lets a dot match a line break as well as any other character</li>
     * <li>{@code u} switches on the unicode character classes</li>
     * </ul>
     * @param name  the name the argument is read back under
     * @param regex the regular expression the text has to match at the start
     * @param flags the flag letters to switch on, from {@code i}, {@code s} and {@code u}
     * @return self for chaining.
     */
    public CommandBuilder regexArgType(String name, String regex, String flags) {
        int fg = 0;
        for (int i = 0; i < flags.length(); ++i) {
            switch (flags.charAt(i)) {
                case 'i':
                    fg += Pattern.CASE_INSENSITIVE;
                    break;
                case 's':
                    fg += Pattern.DOTALL;
                    break;
                case 'u':
                    fg += Pattern.UNICODE_CHARACTER_CLASS;
                    break;
            }
        }
        int finalFg = fg;
        argument(name, () -> new RegexArgType(regex, finalFg));
        return this;
    }

    /**
     * Adds a text-component argument using vanilla command syntax; the parsed component is
     * returned as a {@code TextHelper}.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder textArgType(String name) {
        argument(name, ComponentArgument::textComponent);
        return this;
    }

    /**
     * Adds a duration argument; its parsed value is an integer number of ticks. The input unit
     * may be ticks ({@code t}), seconds ({@code s}), or days ({@code d}); a unit is required.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder timeArg(String name) {
        argument(name, (Supplier<ArgumentType<?>>) TimeArgument::time);
        return this;
    }

    /**
     * Adds a resource-location argument; {@link com.jsmacrosce.jsmacros.client.api.helper.CommandContextHelper#getArg(String)}
     * returns its full identifier as a string.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder identifierArg(String name) {
        argument(name, ResourceLocationArgument::id);
        return this;
    }

    /**
     * Alias for {@link #nbtCompoundArg(String)}; the parsed compound is returned as its NBT helper.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder nbtArg(String name) {
        return nbtCompoundArg(name);
    }

    /**
     * Adds one NBT tag of any type; {@link #nbtCompoundArg(String)} accepts only a compound. The
     * parsed tag is converted to its corresponding NBT helper.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder nbtElementArg(String name) {
        argument(name, NbtTagArgument::nbtTag);
        return this;
    }

    /**
     * Adds an NBT compound tag. The parsed compound is converted to its corresponding NBT helper.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder nbtCompoundArg(String name) {
        argument(name, (CompoundTagArgument::compoundTag));
        return this;
    }

    /**
     * Adds a vanilla color argument; its value is returned as a formatting helper.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder colorArg(String name) {
        argument(name, ColorArgument::color);
        return this;
    }

    /**
     * Adds an angle argument; the parsed value is resolved against the command source and
     * returned as degrees.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder angleArg(String name) {
        argument(name, AngleArgument::new);
        return this;
    }

    /**
     * Adds an item argument. This is an alias for {@link #itemStackArg(String)}; its returned
     * stack always has a count of one.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder itemArg(String name) {
        return itemStackArg(name);
    }

    /**
     * Adds an item argument and returns a stack with a count of one.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder itemStackArg(String name) {
        argument(name, ItemArgument::item);
        return this;
    }

    /**
     * Adds an item predicate argument, returned as a function that tests an item stack.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder itemPredicateArg(String name) {
        argument(name, ItemPredicateArgument::new);
        return this;
    }

    /**
     * Alias for {@link #blockStateArg(String)}; returns the parsed block state as a helper.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder blockArg(String name) {
        return blockStateArg(name);
    }

    /**
     * Adds a block-state argument and returns the parsed state as a helper.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder blockStateArg(String name) {
        argument(name, BlockStateArgument::block);
        return this;
    }

    /**
     * Adds a block predicate argument, returned as a function that tests the block at a position.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder blockPredicateArg(String name) {
        argument(name, BlockPredicateArgument::new);
        return this;
    }

    /**
     * Adds a block position argument and returns its parsed value as a {@link BlockPosHelper}.
     * The coordinates may be absolute, relative ({@code ~}), or local ({@code ^}), resolved
     * against the command source.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder blockPosArg(String name) {
        argument(name, BlockPosArgument::new);
        return this;
    }

    /**
     * Adds a two-coordinate horizontal position argument and returns its parsed value as a
     * {@link BlockPosHelper}. Its Y coordinate is zero; coordinates may be absolute, relative
     * ({@code ~}), or local ({@code ^}), resolved against the command source.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder columnPosArg(String name) {
        argument(name, ColumnPosArgument::new);
        return this;
    }

    /**
     * Adds a dimension argument, returned to scripts as its full identifier string.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder dimensionArg(String name) {
        argument(name, DimensionArgument::new);
        return this;
    }

//    public CommandBuilder enchantmentArg(String name) {
//        argument(name, EnchantmentA::new);
//        return this;
//    }
//
//    public CommandBuilder entityTypeArg(String name) {
//        argument(name, EntitySummonArgumentType::new);
//        suggests(SuggestionProviders.SUMMONABLE_ENTITIES);
//        return this;
//    }

    //TODO: Add client side EntitySelector, because the default one requires a server world.

    /**
     * Adds a named slot argument. The name must resolve in the game's slot-range list and select
     * exactly one slot; multi-slot ranges are rejected. The parsed value is that slot's integer
     * index. Available names are defined by the target Minecraft version; for example,
     * {@code hotbar.0} selects the first hotbar slot, while {@code hotbar.*} is a multi-slot
     * range and is rejected.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder itemSlotArg(String name) {
        argument(name, SlotArgument::new);
        return this;
    }

    /**
     * Adds a particle argument, returned to scripts as its particle registry id.
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder particleArg(String name) {
        argument(name, ParticleArgument::new);
        return this;
    }

//    public CommandBuilder statusEffectArg(String name) {
//        argument(name, StatusEffectArgumentType::new);
//        return this;
//    }

    /**
     * Attaches a callback to the innermost command node. Its return value is ignored; after it
     * runs, the command reports success. Use
     * {@link com.jsmacrosce.jsmacros.core.library.impl.FJsMacros#runScript(String, BaseEvent)}
     * when the callback needs waits; the {@link CommandContextHelper} is a {@link BaseEvent} and
     * can be passed to that method.
     *
     * @param callback the command callback
     * @return this builder, for chaining
     */
    public abstract CommandBuilder executes(MethodWrapper<CommandContextHelper, Object, Object, ?> callback);

    protected abstract <S> void suggests(SuggestionProvider<S> suggestionProvider);

    /**
     * @param suggestions strings offered as completions, without identifier parsing
     * @return self for chaining
     * @since 1.6.5
     */
    public CommandBuilder suggestMatching(String... suggestions) {
        return suggestMatching(Arrays.asList(suggestions));
    }

    /**
     * @param suggestions strings offered as completions, without identifier parsing
     * @return self for chaining.
     * @since 1.8.4
     */
    public CommandBuilder suggestMatching(Collection<String> suggestions) {
        suggests((ctx, builder) -> SharedSuggestionProvider.suggest(suggestions, builder));
        return this;
    }

    /**
     * @param suggestions identifiers offered as completions
     * @return self for chaining
     * @since 1.6.5
     */
    public CommandBuilder suggestIdentifier(String... suggestions) {
        return suggestIdentifier(Arrays.asList(suggestions));
    }

    /**
     * @param suggestions identifiers offered as completions after parsing as resource locations
     * @return self for chaining.
     * @since 1.8.4
     */
    public CommandBuilder suggestIdentifier(Collection<String> suggestions) {
        suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(suggestions.stream().map(ResourceLocation::parse), builder));
        return this;
    }

    /**
     * Suggests these block positions as space-separated integer coordinates.
     * @param positions the block positions to suggest
     * @return self for chaining.
     * @since 1.8.4
     */
    public CommandBuilder suggestBlockPositions(BlockPosHelper... positions) {
        return suggestPositions(Arrays.stream(positions).map(b -> b.getX() + " " + b.getY() + " " + b.getZ()).collect(Collectors.toList()));
    }

    /**
     * Suggests these block positions as space-separated integer coordinates.
     * @param positions the block positions to suggest
     * @return self for chaining.
     * @since 1.8.4
     */
    public CommandBuilder suggestBlockPositions(Collection<BlockPosHelper> positions) {
        return suggestPositions(positions.stream().map(b -> b.getX() + " " + b.getY() + " " + b.getZ()).collect(Collectors.toList()));
    }

    /**
     * Suggests positions expressed as three space-separated coordinate components.
     *
     * @param positions the positions to suggest
     * @return self for chaining.
     * @since 1.8.4
     */
    public CommandBuilder suggestPositions(String... positions) {
        return suggestPositions(Arrays.asList(positions));
    }

    /**
     * Suggests positions expressed as three space-separated coordinate components. Each input
     * must contain at least three components; they are parsed without validation here.
     *
     * @param positions the positions to suggest
     * @return self for chaining.
     * @since 1.8.4
     */
    public CommandBuilder suggestPositions(Collection<String> positions) {
        suggests((ctx, builder) -> SharedSuggestionProvider.suggestCoordinates(builder.getRemaining(), positions.stream().map(p -> {
                    String[] split = p.split(" ");
                    return new SharedSuggestionProvider.TextCoordinates(split[0], split[1], split[2]);
                }).collect(Collectors.toList()), builder, s -> true)
        );
        return this;
    }

    /**
     * Uses the callback to add suggestions to the current suggestions builder.
     * @param callback callback that receives the command context and suggestions builder
     * @return self for chaining
     * @since 1.6.5
     */
    public CommandBuilder suggest(MethodWrapper<CommandContextHelper, SuggestionsBuilderHelper, Object, ?> callback) {
        suggests((ctx, builder) -> {
            callback.accept(new CommandContextHelper(ctx), new SuggestionsBuilderHelper(builder));
            return builder.buildFuture();
        });
        return this;
    }

    protected <S> int internalExecutes(CommandContext<S> context, MethodWrapper<CommandContextHelper, Object, Object, ?> callback) {
        EventContainer<?> lock = new EventContainer<>(callback.getCtx());
        lock.setLockThread(Thread.currentThread());
        EventLockWatchdog.startWatchdog(lock, new IEventListener() {
            @Override
            public boolean joined() {
                return false;
            }

            @Override
            public EventContainer<?> trigger(BaseEvent event) {
                return null;
            }

            @Override
            public String toString() {
                return "CommandBuilder{\"called_by\": " + callback.getCtx().getTriggeringEvent().toString() + "}";
            }
        }, JsMacrosClient.clientCore.config.getOptions(CoreConfigV2.class).maxLockTime);
        try {
            CommandContextHelper commandEvent = new CommandContextHelper(context);
            commandEvent.trigger();
            callback.accept(commandEvent);
        } finally {
            lock.releaseLock();
        }
        return 1;
    }

    /**
     * Merges the innermost argument branch one level upward, so the next argument is attached at
     * that parent level rather than beneath the innermost argument.
     * @return self for chaining.
     */
    public abstract CommandBuilder or();

    /**
     * name overload for {@link #or()} to work around language keyword restrictions
     *
     * @return self for chaining
     * @since 1.5.2
     */
    public CommandBuilder otherwise() {
        or();
        return this;
    }

    /**
     * Merges branches upward until the command tree has the requested depth, counting the root as depth 1.
     * Values below 1 are treated as 1; a depth greater than the current depth leaves the tree unchanged.
     * @param argumentLevel depth to retain after merging
     * @return self for chaining.
     */
    public abstract CommandBuilder or(int argumentLevel);

    /**
     * Name overload for {@link #or(int)} to work around language keyword restrictions.
     *
     * @param argLevel depth to retain after merging branches, counting the root as depth 1; values below 1 are treated as 1
     * @return self for chaining
     * @since 1.5.2
     */
    public CommandBuilder otherwise(int argLevel) {
        or(argLevel);
        return this;
    }

    private static class RegexArgType implements ArgumentType<String[]> {

        Pattern pattern;

        public RegexArgType(String regex, int flags) {
            this.pattern = Pattern.compile(regex, flags);
        }

        @Override
        public String[] parse(StringReader reader) throws CommandSyntaxException {
            int i = reader.getCursor();
            Matcher m = pattern.matcher(reader.getRemaining());
            if (m.find() && m.start() == 0) {
                String[] args = new String[m.groupCount() + 1];
                for (int j = 0; j < args.length; ++j) {
                    args[j] = m.group(j);
                }
                reader.setCursor(i + m.group(0).length());
                return args;
            } else {
                throw new SimpleCommandExceptionType(Component.translatable(
                        "jsmacrosce.commandfailedregex",
                        "/" + pattern.pattern() + "/"
                )).createWithContext(reader);
            }
        }

    }

    /**
     * Registers this command with the client.
     *
     * @return self for chaining.
     */
    @Override
    public abstract CommandBuilder register();

    /**
     * Unregisters this command.
     *
     * @return self for chaining.
     * @since 1.6.5
     */
    @Override
    public abstract CommandBuilder unregister() throws IllegalAccessException;

}
