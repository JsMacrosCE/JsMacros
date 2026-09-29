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
 * a builder for a command the script registers on the client, the thing
 * {@link com.jsmacrosce.jsmacros.client.api.library.impl.FChat#getCommandManager()} hands out.
 * <p>
 * The command is built by nesting rather than by listing a path. The command name is the root,
 * and every argument method adds one more level <i>inside</i> whatever came before it, so the
 * order the methods are called in is the order the player types them in. That is what makes
 * {@code or()} necessary: two things can only be alternatives if they sit at the same level, and
 * the only way to get two nodes onto the same level is to build them one after the other and
 * merge them. Merging moves the point the next argument hangs off, and how far it moves is what
 * the two forms of {@code or} differ over.
 * <p>
 * {@code executes} attaches to the innermost node, not to the command as a whole, so a command
 * with arguments needs one {@code executes} per branch it can finish on. Reading an argument's
 * value back is done on the
 * {@link com.jsmacrosce.jsmacros.client.api.helper.CommandContextHelper} the callback is handed,
 * with {@code getArg}, and what comes back depends on the argument method that declared it. Each
 * one's own javadoc says what that is.
 * <p>
 * Nothing is sent to the game until {@link #register()}, and a builder is spent by it:
 * registering takes the whole thing off the builder's own stack, so the same builder cannot be
 * registered a second time. Registering the same command again means building a new builder for
 * the name and calling {@link #register()} on that one.
 * example:
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
     * adds a word the player has to type exactly, with no value of its own.
     * <p>
     * This is the only argument method that stores nothing to read back: a literal is a fixed
     * word, so {@code getArg} on its name throws rather than returning anything. It is what a
     * command is mostly made of, and it is also the one kind of argument a completion callback
     * cannot be attached to, because the builder rejects suggestions on a literal.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("count");
     * // /count set 10
     * command.literalArg("set");
     * command.register();
     * </pre>
     * @param name the word the player types
     * @return self for chaining.
     */
    public abstract CommandBuilder literalArg(String name);

    /**
     * adds a bare {@code true} or {@code false}.
     * <p>
     * The player types one of those two words, and a script reads the value back as a boolean.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("gamemode");
     * // /gamemode creative true
     * command.literalArg("creative").booleanArg("on");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder booleanArg(String name) {
        argument(name, BoolArgumentType::bool);
        return this;
    }

    /**
     * adds a whole number with no bound on either end.
     * <p>
     * The number has to fit in a 32 bit signed integer, so the practical range is
     * {@code -2147483648} to {@code 2147483647}. For anything narrower,
     * {@link #intArg(String, int, int)} is the one to reach for, since it reports the range to
     * the player as they type instead of failing after the fact.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("count");
     * // /count set 10
     * command.literalArg("set").intArg("amount");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder intArg(String name) {
        argument(name, (Supplier<ArgumentType<?>>) IntegerArgumentType::integer);
        return this;
    }

    /**
     * adds a whole number that has to be between two values.
     * <p>
     * Both ends are inclusive, and a number outside them is rejected while it is being typed,
     * which is what makes this better than the unbounded form for anything a player is meant to
     * type often. A script reads the value back as a number.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("count");
     * // /count set 10, and the player cannot type 0 or 65
     * command.literalArg("set").intArg("amount", 1, 64);
     * command.register();
     * </pre>
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
     * adds a whole number <i>or</i> a range of them, written with a dot between the ends.
     * <p>
     * This is a different shape from {@link #intArg(String, int, int)}. It is a single argument
     * that accepts either form, so the player can type {@code 10} or {@code 10..20} and the
     * command sees one value either way. The value comes back as the raw brigadier range object
     * rather than as a number, because there is no single number to hand back; the object holds
     * both ends.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("count");
     * // /count set 10 or /count set 10..20
     * command.literalArg("set").intRangeArg("amount");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder intRangeArg(String name) {
        argument(name, RangeArgument::intRange);
        return this;
    }

    /**
     * adds a whole number with no bound, in the wider 64 bit form.
     * <p>
     * This is the same idea as {@link #intArg(String)} with a range too wide for a 32 bit
     * integer. For most numbers a player types there is no reason to prefer it over the int form,
     * so reach for it when the value genuinely does not fit.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("count");
     * command.literalArg("set").longArg("amount");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder longArg(String name) {
        argument(name, (Supplier<ArgumentType<?>>) LongArgumentType::longArg);
        return this;
    }

    /**
     * adds a 64 bit whole number that has to be between two values.
     * <p>
     * Both ends are inclusive, and a number outside them is rejected while it is being typed.
     * This is the wide counterpart to {@link #intArg(String, int, int)}, not a different idea.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("count");
     * command.literalArg("set").longArg("amount", 1, 1000000);
     * command.register();
     * </pre>
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
     * adds a number <i>or</i> a range of them, in the form that accepts decimals.
     * <p>
     * The counterpart to {@link #intRangeArg(String)}, and the same single-argument-two-shapes
     * idea: {@code 1.5} and {@code 1.5..3.5} are both accepted. The value comes back as the raw
     * brigadier range object rather than as a number.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("scale");
     * // /scale 1.5 or /scale 1.5..3.5
     * command.floatRangeArg("factor");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder floatRangeArg(String name) {
        argument(name, RangeArgument::floatRange);
        return this;
    }

    /**
     * adds a decimal number with no bound on either end.
     * <p>
     * A script reads the value back as a number. This is the decimal counterpart to
     * {@link #intArg(String)}; the two are separate methods because the bounds mean different
     * things for each, so a command cannot quietly take a decimal where it meant a whole number.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("scale");
     * // /scale 1.5
     * command.doubleArg("factor");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder doubleArg(String name) {
        argument(name, (Supplier<ArgumentType<?>>) DoubleArgumentType::doubleArg);
        return this;
    }

    /**
     * adds a decimal number that has to be between two values.
     * <p>
     * Both ends are inclusive, and a number outside them is rejected while it is being typed. The
     * decimal counterpart to {@link #intArg(String, int, int)}.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("scale");
     * command.doubleArg("factor", 0.5, 4.0);
     * command.register();
     * </pre>
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
     * adds a UUID, written the way a UUID is usually written.
     * <p>
     * The value comes back as the raw java {@code UUID} object rather than as a string, because
     * this argument type parses into one and nothing converts it afterwards.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("lookup");
     * // /lookup 00000000-0000-0000-0000-000000000000
     * command.uuidArgType("id");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder uuidArgType(String name) {
        argument(name, UuidArgument::uuid);
        return this;
    }

    /**
     * adds text that runs to the end of what the player typed.
     * <p>
     * No quoting is needed and a space does not end it, which makes this the one to reach for
     * when the argument is a sentence. It has to be the last argument on its branch, because it
     * consumes everything after it, and anything the builder adds afterwards would be
     * unreachable.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("say");
     * // /say hello there, this is all one argument
     * command.greedyStringArg("message");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder greedyStringArg(String name) {
        argument(name, StringArgumentType::greedyString);
        return this;
    }

    /**
     * adds text in quotes, which is what lets it contain spaces.
     * <p>
     * This is the counterpart to {@link #greedyStringArg(String)} for the case where the argument
     * has to be the last one but must still stop at a boundary. A script reads it back as a
     * string with the quotes already taken off.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("nick");
     * // /nick "a name with spaces"
     * command.quotedStringArg("name");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder quotedStringArg(String name) {
        argument(name, StringArgumentType::string);
        return this;
    }

    /**
     * adds a single word, with no spaces allowed in it.
     * <p>
     * This is the plainest text form and the one to use for a name or an id, where anything with
     * a space in it would be a mistake. A script reads it back as a string.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("nick");
     * // /nick steve
     * command.wordArg("name");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder wordArg(String name) {
        argument(name, StringArgumentType::word);
        return this;
    }

    /**
     * adds text matched against a regular expression, and hands the capture groups to the
     * script as an array.
     * <p>
     * This is the one argument method that is not a vanilla argument type, so it is worth knowing
     * exactly how it behaves. The pattern has to match at the very start of what the player has
     * left to type, not anywhere inside it, and what is consumed is the length of the whole
     * match. So a pattern that can match an empty string consumes nothing, and a pattern that
     * does not match there is an error rather than a skip.
     * <p>
     * The value comes back as an array whose first element is the whole match and whose
     * remaining elements are the capture groups in order, so an expression with no groups gives
     * back a one element array and a script should read the groups by index from there. A
     * pattern that fails reports the expression back to the player, written between slashes.
     * <p>
     * The {@code flags} string is read one character at a time and the characters are added up,
     * so order does not matter and a character that is not one of the three is quietly ignored
     * rather than rejected:
     * <ul>
     * <li>{@code i} makes the match ignore case
     * <li>{@code s} lets a dot match a line break as well as any other character
     * <li>{@code u} switches on the unicode character classes
     * </ul>
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("shout");
     * // /shout HELLO 12  -- two groups, matched case-insensitively
     * command.regexArgType("shout", "(\\w+) (\\d+)", "i");
     * command.register();
     * </pre>
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
     * adds a text component, in the same forms the vanilla commands accept.
     * <p>
     * A script reads the value back as a {@code TextHelper} rather than as the raw brigadier
     * object, so it can be handed straight to the chat helpers.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("shout");
     * command.textArgType("message");
     * command.executes(JavaWrapper.methodToJava(function (ctx) {
     *   Chat.log(ctx.getArg("message"));
     *   return true;
     * }));
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder textArgType(String name) {
        argument(name, ComponentArgument::textComponent);
        return this;
    }

    /**
     * adds a length of time, written with a unit suffix.
     * <p>
     * The value comes back as a number of ticks, whatever unit the player typed, so a script
     * does not have to work out what {@code 5s} or {@code 2m} turned into.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("wait");
     * // /wait 5s
     * command.timeArg("duration");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder timeArg(String name) {
        argument(name, (Supplier<ArgumentType<?>>) TimeArgument::time);
        return this;
    }

    /**
     * adds a namespaced id, the {@code namespace:path} form things like items and blocks use.
     * <p>
     * A script reads the value back as a plain string holding the full id, so it is ready to
     * pass to the registry helpers without unwrapping anything.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("give");
     * // /give minecraft:diamond
     * command.identifierArg("id");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder identifierArg(String name) {
        argument(name, ResourceLocationArgument::id);
        return this;
    }

    /**
     * adds a compound NBT tag, written the same way as in a command.
     * <p>
     * This is a second name for {@link #nbtCompoundArg(String)} and does exactly the same thing;
     * it is here because the vanilla command that reads a whole tag is the one a script is most
     * likely to reach for. A script reads the value back as the matching NBT helper.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("summon");
     * command.literalArg("horse").nbtArg("data");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder nbtArg(String name) {
        return nbtCompoundArg(name);
    }

    /**
     * adds a single NBT tag of any type, not just a compound one.
     * <p>
     * The difference from {@link #nbtCompoundArg(String)} is what the player is allowed to type.
     * A compound tag here is written the same way as a plain one, and the type of the value that
     * comes back is whatever was typed, so a script has to look at it rather than assume.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("summon");
     * command.literalArg("horse").nbtElementArg("data");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder nbtElementArg(String name) {
        argument(name, NbtTagArgument::nbtTag);
        return this;
    }

    /**
     * adds a compound NBT tag, written the same way as in a command.
     * <p>
     * A compound tag is the braced form, so this only accepts a whole {@code {..}} block. For a
     * single tag of any other type, {@link #nbtElementArg(String)} is the one. A script reads
     * the value back as the matching NBT helper.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("summon");
     * // /summon horse {NoAI:1b}
     * command.literalArg("horse").nbtCompoundArg("data");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder nbtCompoundArg(String name) {
        argument(name, (CompoundTagArgument::compoundTag));
        return this;
    }

    /**
     * adds a colour name, the same list the vanilla commands take.
     * <p>
     * A script reads the value back as a formatting helper rather than as the raw chat
     * formatting, so it can be handed straight to the text helpers.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("colour");
     * // /colour red
     * command.colorArg("colour");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder colorArg(String name) {
        argument(name, ColorArgument::color);
        return this;
    }

    /**
     * adds an angle, in degrees or with a compass direction in front of it.
     * <p>
     * An angle is resolved rather than just parsed, because a direction is relative to whoever
     * typed it, and a position is resolved for the same reason. A script reads the value back as
     * a plain number of degrees.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("look");
     * // /look 90 or /look north
     * command.angleArg("angle");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder angleArg(String name) {
        argument(name, AngleArgument::new);
        return this;
    }

    /**
     * adds an item, written the same way as in the vanilla commands.
     * <p>
     * This is a second name for {@link #itemStackArg(String)} and does exactly the same thing.
     * Worth knowing when reading the value back: whatever count the player typed, a script gets
     * a stack of <b>one</b> item, because this argument only ever parses an item and never a
     * stack.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("give");
     * // /give minecraft:diamond
     * command.itemArg("item");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder itemArg(String name) {
        return itemStackArg(name);
    }

    /**
     * adds an item, written the same way as in the vanilla commands.
     * <p>
     * Worth knowing when reading the value back: whatever count the player typed, a script gets
     * a stack of <b>one</b> item, because this argument only ever parses an item and never a
     * stack. The name says stack because that is what the vanilla command is called, not because
     * a count is carried through.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("give");
     * // /give minecraft:diamond
     * command.itemStackArg("item");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder itemStackArg(String name) {
        argument(name, ItemArgument::item);
        return this;
    }

    /**
     * adds a test for an item, rather than a particular item.
     * <p>
     * The player types the same form as an ordinary item argument, and the difference is what
     * comes back: a script gets a function that takes an item and answers whether it is
     * accepted, which is a different thing from an item. That makes this the argument to reach
     * for when the player is naming a <i>kind</i> of thing rather than one of them.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("purge");
     * // /purge minecraft:logs
     * command.itemPredicateArg("target");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder itemPredicateArg(String name) {
        argument(name, ItemPredicateArgument::new);
        return this;
    }

    /**
     * adds a block, written the same way as in the vanilla commands.
     * <p>
     * This is a second name for {@link #blockStateArg(String)} and does exactly the same thing;
     * it is here because the vanilla command that reads a block is the one a script is most
     * likely to reach for. A script reads the value back as a block state helper.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("setblock");
     * // /setblock ~ ~ ~ minecraft:stone
     * command.blockArg("block");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder blockArg(String name) {
        return blockStateArg(name);
    }

    /**
     * adds a block, written the same way as in the vanilla commands.
     * <p>
     * The player may narrow it to a particular state, so the value a script reads back is the
     * block <i>and</i> that state, as a block state helper.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("setblock");
     * // /setblock ~ ~ ~ minecraft:oak_stairs[facing=north]
     * command.blockStateArg("block");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder blockStateArg(String name) {
        argument(name, BlockStateArgument::block);
        return this;
    }

    /**
     * adds a test for a block, rather than a particular block.
     * <p>
     * The player types the same form as an ordinary block argument, and the difference is what
     * comes back: a script gets a function that takes a block position and answers whether the
     * block there is accepted, which is a different thing from a block.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("strip");
     * // /strip minecraft:logs
     * command.blockPredicateArg("target");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder blockPredicateArg(String name) {
        argument(name, BlockPredicateArgument::new);
        return this;
    }

    /**
     * adds a block position, in the {@code x y z} form with the relative selectors allowed.
     * <p>
     * A script reads the value back as a block position helper, already resolved to absolute
     * coordinates, so {@code ~} and {@code ^} have been applied against the player who typed it.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("mark");
     * // /mark 10 64 -3
     * command.blockPosArg("pos");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder blockPosArg(String name) {
        argument(name, BlockPosArgument::new);
        return this;
    }

    /**
     * adds a horizontal position, written as just an x and a z with no y.
     * <p>
     * This is the counterpart to {@link #blockPosArg(String)} for the commands that work on a
     * column rather than a block, and the player types two numbers instead of three. A script
     * still reads it back as a block position helper, but the y is not something the player
     * chose: it is fixed at 0, so use the x and the z and ignore the y.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("column");
     * // /column 10 -3
     * command.columnPosArg("pos");
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder columnPosArg(String name) {
        argument(name, ColumnPosArgument::new);
        return this;
    }

    /**
     * adds a dimension, taken from the list the connected server offers.
     * <p>
     * A script reads the value back as a plain string holding the full id, so it is ready to
     * pass to a lookup that takes one.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("tpdim");
     * // /tpdim minecraft:the_nether
     * command.dimensionArg("dim");
     * command.register();
     * </pre>
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
     * adds a named slot, the form the vanilla slot arguments take.
     * <p>
     * This is not a bare number checked against whichever menu is open. The text is looked up in
     * the game's own list of named slot ranges, and two things then have to hold: the name has
     * to be one that list knows, and the range it names has to cover exactly one slot. A name
     * that means more than one slot is refused rather than quietly taking the first, so this is
     * not a way to ask for a whole hotbar.
     * <p>
     * A script reads the value back as a number, and that number is the single slot the name
     * resolved to. The name has to come from the game's own list, which is fixed and does
     * enumerate: 1.21.8 registers 165 names.
     * <p>
     * 151 of them come from seven prefixes, each of which expands into one name per slot plus a
     * single multi-slot name for the family as a whole. The number in a name is the offset
     * within the family, and the slot it resolves to is that offset added to the family's base:
     * {@code container.} runs {@code 0}-{@code 53} from a base of 0, {@code hotbar.}
     * {@code 0}-{@code 8} from 0, {@code inventory.} {@code 0}-{@code 26} from 9,
     * {@code enderchest.} {@code 0}-{@code 26} from 200, {@code villager.}
     * {@code 0}-{@code 7} from 300, {@code horse.} {@code 0}-{@code 14} from 500 and
     * {@code player.crafting.} {@code 0}-{@code 3} from 500. Each family also has a
     * {@code something.*} name covering all of it at once, and those are the ones that do not
     * parse, because they name more than one slot.
     * <p>
     * The other 14 are plain names: {@code contents} and {@code weapon}, which are the only two
     * with no full stop in them, then {@code weapon.mainhand}, {@code weapon.offhand} and
     * {@code weapon.*}, the five armour slots {@code armor.head}, {@code armor.chest},
     * {@code armor.legs}, {@code armor.feet} and {@code armor.body} plus {@code armor.*}, and
     * finally {@code saddle}, {@code horse.chest} and {@code player.cursor}. Everything in that
     * list except the two {@code .*} entries names exactly one slot.
     * <p>
     * The family names on their own are not registered, which is the easy mistake here: the bare
     * {@code hotbar}, {@code container}, {@code inventory}, {@code villager}, {@code horse},
     * {@code enderchest}, {@code armor} and {@code player} all fail to resolve, while
     * {@code hotbar.0} and {@code armor.head} resolve.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("click");
     * command.itemSlotArg("slot")
     * .executes(JavaWrapper.methodToJava(function (ctx) {
     *   // the name the player typed has been resolved to one slot number
     *   const slot = ctx.getArg("slot");
     *   Chat.log(`that is slot ${slot}`);
     *   return true;
     * }));
     * command.register();
     * </pre>
     * @param name the name the argument is read back under
     * @return self for chaining.
     */
    public CommandBuilder itemSlotArg(String name) {
        argument(name, SlotArgument::new);
        return this;
    }

    /**
     * adds a particle, taken from the list the game knows.
     * <p>
     * A script reads the value back as a plain string holding the particle id, so it can be
     * compared against the ids in the registry without unwrapping anything.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("p");
     * // /p minecraft:flame
     * command.particleArg("particle");
     * command.register();
     * </pre>
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
     * it is recommended to use {@link com.jsmacrosce.jsmacros.core.library.impl.FJsMacros#runScript(String, BaseEvent)}
     * in the callback if you expect to actually do anything complicated with waits.
     * <p>
     * the {@link CommandContextHelper} arg is an {@link BaseEvent}
     * so you can pass it directly to {@link com.jsmacrosce.jsmacros.core.library.impl.FJsMacros#runScript(String, BaseEvent)}.
     * <p>
     * make sure your callback returns a boolean success = true.
     *
     * @param callback
     * @return
     */
    public abstract CommandBuilder executes(MethodWrapper<CommandContextHelper, Object, Object, ?> callback);

    protected abstract <S> void suggests(SuggestionProvider<S> suggestionProvider);

    /**
     * @param suggestions
     * @return
     * @since 1.6.5
     */
    public CommandBuilder suggestMatching(String... suggestions) {
        return suggestMatching(Arrays.asList(suggestions));
    }

    /**
     * @param suggestions the strings to match
     * @return self for chaining.
     * @since 1.8.4
     */
    public CommandBuilder suggestMatching(Collection<String> suggestions) {
        suggests((ctx, builder) -> SharedSuggestionProvider.suggest(suggestions, builder));
        return this;
    }

    /**
     * @param suggestions
     * @return
     * @since 1.6.5
     */
    public CommandBuilder suggestIdentifier(String... suggestions) {
        return suggestIdentifier(Arrays.asList(suggestions));
    }

    /**
     * @param suggestions the identifiers to match
     * @return self for chaining.
     * @since 1.8.4
     */
    public CommandBuilder suggestIdentifier(Collection<String> suggestions) {
        suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(suggestions.stream().map(ResourceLocation::parse), builder));
        return this;
    }

    /**
     * @param positions the positions to suggest
     * @return self for chaining.
     * @since 1.8.4
     */
    public CommandBuilder suggestBlockPositions(BlockPosHelper... positions) {
        return suggestPositions(Arrays.stream(positions).map(b -> b.getX() + " " + b.getY() + " " + b.getZ()).collect(Collectors.toList()));
    }

    /**
     * @param positions the positions to suggest
     * @return self for chaining.
     * @since 1.8.4
     */
    public CommandBuilder suggestBlockPositions(Collection<BlockPosHelper> positions) {
        return suggestPositions(positions.stream().map(b -> b.getX() + " " + b.getY() + " " + b.getZ()).collect(Collectors.toList()));
    }

    /**
     * Positions are strings of the form "x y z" where x, y, and z are numbers or the default
     * minecraft selectors "~" and "^" followed by a number.
     *
     * @param positions the positions to suggest
     * @return self for chaining.
     * @since 1.8.4
     */
    public CommandBuilder suggestPositions(String... positions) {
        return suggestPositions(Arrays.asList(positions));
    }

    /**
     * Positions are strings of the form "x y z" where x, y, and z are numbers or the default
     * minecraft selectors "~" and "^" followed by a number.
     *
     * @param positions the positions to match
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
     * @param callback
     * @return
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
            callback.accept(new CommandContextHelper(context));
        } finally {
            lock.releaseLock();
        }
        return 1;
    }

    /**
     * merges the innermost argument into the one above it, so the next argument comes out as a
     * sibling of the last one rather than a child of it.
     * <p>
     * Every argument method nests one level deeper, so two branches built one after the other
     * come out as one below the other and only the first can ever be typed. This puts them side
     * by side. One call climbs exactly <i>one</i> level, so how far the next argument ends up from
     * the command name depends on how deep the branch already built is: a branch that is one
     * level deep needs a single call, a two-level branch needs one call per level, and a branch
     * that is three deep is still one short of the command name after two calls. {@link #or(int)}
     * says the same thing as a number of levels in one call. It acts on whatever is innermost at
     * the moment it is called, which is why it has to go between the branches rather than at the
     * end.
     * <p>
     * Calling it when there is nothing left to merge is not the same on every loader, and a
     * script that relies on one of them will not port. The Fabric implementation does nothing
     * and returns the builder; the NeoForge implementation throws an {@link AssertionError}
     * instead. Since the argument methods are the only thing that pushes onto the stack, the
     * case that reaches this is a builder that has had no arguments added at all, so guarding
     * it by checking that at least one argument was added is enough to stay clear of both.
     * <p>
     * The usual shape is to build a branch to its end, call this, then build the next one. Here
     * the branch is two levels deep, so one call puts the next literal alongside {@code amount}
     * rather than alongside {@code set}:
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("count");
     * // /count set 10
     * command.literalArg("set").intArg("amount", 1, 64);
     * // the next literal is an alternative, not an argument of "amount", so it
     * // is a sibling of "amount" under "set"
     * command.or();
     * // /count set show
     * command.literalArg("show");
     * command.register();
     * </pre>
     * @return self for chaining.
     */
    public abstract CommandBuilder or();

    /**
     * name overload for {@link #or()} to work around language keyword restrictions
     *
     * @return
     * @since 1.5.2
     */
    public CommandBuilder otherwise() {
        or();
        return this;
    }

    /**
     * merges repeatedly, so an alternative can be put several levels up rather than one at a
     * time.
     * <p>
     * This is {@link #or()} applied until the builder is left the given number of levels deep,
     * so the next argument hangs off the node at that depth instead of off the innermost one.
     * The count is the depth that is kept, not the number of calls: {@code or(2)} on a branch
     * three levels deep merges twice and lands the next argument alongside the two it already
     * has, whereas {@code or(1)} merges all the way and puts it alongside the command name
     * itself, which is what the class example does. A level of 1 is also what {@link #register()}
     * does on its own, and a level deeper than the branch has no merges left to do and changes
     * nothing.
     * <p>
     * A level below 1 is not rejected, it is treated as 1, so the command collapses to the top
     * either way.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("count");
     * // /count set 10 64
     * command.literalArg("set").intArg("from").intArg("to");
     * // put what follows alongside "from" and "to", under "set"
     * command.or(2);
     * // /count set show
     * command.literalArg("show");
     * command.register();
     * </pre>
     * @param argumentLevel how many levels to merge up; anything below 1 means all the way up
     * @return self for chaining.
     */
    public abstract CommandBuilder or(int argumentLevel);

    /**
     * name overload for {@link #or(int)} to work around language keyword restrictions
     *
     * @param argLevel
     * @return
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
     * sends the command to the game, and hands the builder back.
     * <p>
     * This is the step that actually does something; everything before it only builds a
     * description. Two things happen here that are worth knowing about. The builder first pulls
     * the whole chain up to the command name, so whatever was left dangling is attached and
     * there is nothing left to add, which is why {@code register()} has to be the last call.
     * It also <i>uses up</i> the builder: the finished command is taken off the builder's own
     * stack, so registering the same builder twice fails rather than registering twice. Build a
     * new builder for the name to put the command back.
     * <p>
     * Nothing is registered while the client is not connected to a server, because there is no
     * command tree to add to. What is built is kept either way and is registered by the next
     * client-command registration event, so a command built before joining a world still turns
     * up afterwards.
     * <p>
     * Registering a name that is already taken does not swap the old command out wholesale: the
     * new command's handler takes over if it has one, and its sub-arguments are added to the
     * ones already there. To take a command back out instead, {@link #unregister()} is the
     * counterpart, or {@code Chat.getCommandManager()}'s {@code unregisterCommand}.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("count");
     * command.literalArg("set").intArg("amount", 1, 64)
     * .executes(JavaWrapper.methodToJava(function (ctx) {
     *   Chat.log(`the amount is now ${ctx.getArg("amount")}`);
     *   return true;
     * }));
     * // fold the branch up to the command name before starting the next one
     * command.or(1);
     * command.or(1);
     * // /count show
     * command.literalArg("show")
     * .executes(JavaWrapper.methodToJava(function (ctx) {
     *   Chat.log("showing");
     *   return true;
     * }));
     * // nothing has reached the game until this line
     * command.register();
     * </pre>
     *
     * @return self for chaining.
     */
    @Override
    public abstract CommandBuilder register();

    /**
     * removes this command.
     *
     * @since 1.6.5
     */
    @Override
    public abstract CommandBuilder unregister() throws IllegalAccessException;

}
