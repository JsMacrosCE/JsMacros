package com.jsmacrosce.jsmacros.client.api.helper;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.ResourceLocation;

import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.world.BlockPosHelper;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * the tab-completion list a command contributes while the player is typing it.
 * <p>
 * This is the object a command hands to a script's completion callback, and everything on it is
 * about <i>what to offer</i>. The four readers — {@link #getInput()}, {@link #getStart()},
 * {@link #getRemaining()} and {@link #getRemainingLowerCase()} — describe what the player has
 * typed so far, which is what decides which suggestions are worth offering; the
 * {@code suggest...} calls add entries to the list the client shows.
 * <p>
 * A suggestion is not a whole word but a <b>replacement for the tail of what is typed</b>. The
 * start index says where that tail begins, and every entry added replaces the text from there
 * to the end, so completing {@code /tp ho} with a start index pointing at the {@code ho} and a
 * suggestion of {@code home} leaves {@code /tp home}. A suggestion is also dropped if it is
 * exactly what is already typed, since replacing text with itself would show nothing new.
 * <p>
 * The callback takes two arguments, the parsed command context and this helper, and it has to
 * add at least one suggestion for anything to appear:
 * <pre>
 * const command = Chat.getCommandManager().createCommandBuilder("teleport")
 *   .literalArg("home")
 *   .suggest(JavaWrapper.methodToJava(function (ctx, suggestions) {
 *     // what the player has typed, and where the part being completed starts
 *     if (suggestions.getRemaining().length === 0) {
 *       suggestions.suggest("spawn").suggest("base").suggest(3);
 *     }
 *   }));
 * command.register();
 * </pre>
 *
 * @since 1.6.5
 */
@SuppressWarnings("unused")
@DocletCategory("Misc Helpers")
public class SuggestionsBuilderHelper extends BaseHelper<SuggestionsBuilder> {
    public SuggestionsBuilderHelper(SuggestionsBuilder base) {
        super(base);
    }

    /**
     * the whole line the player has typed so far, completion request and all.
     * <p>
     * This is the argument the command was invoked with, not just the part after the command
     * name, and it is what the range of every suggestion this builder produces is measured
     * against. {@link #getStart()} is the index into this string where the part being completed
     * begins.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("teleport")
     *   .literalArg("home")
     *   .suggest(JavaWrapper.methodToJava(function (ctx, suggestions) {
     *     // the full line, e.g. "/teleport home sp"
     *     Chat.log(`typed: ${suggestions.getInput()}`);
     *     // add a suggestion to replace everything after the start index
     *     suggestions.suggest("spawn");
     *   }));
     * command.register();
     * </pre>
     *
     * @return the full input line the suggestions are for
     * @since 1.6.5
     */
    public String getInput() {
        return base.getInput();
    }

    /**
     * the index in {@link #getInput()} where the part being completed begins.
     * <p>
     * Every suggestion this builder adds replaces the input from this index onwards, so this is
     * the boundary a script cares about when it wants to complete only the last word and leave
     * the rest of the line alone. It is an index into the full input, so a line of
     * {@code "/teleport home sp"} with the completion starting at the {@code sp} gives 15.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("teleport")
     *   .literalArg("home")
     *   .suggest(JavaWrapper.methodToJava(function (ctx, suggestions) {
     *     // the part after the start index is the word being completed
     *     const word = suggestions.getInput().substring(suggestions.getStart());
     *     if (word === "sp") {
     *       suggestions.suggest("spawn");
     *     }
     *   }));
     * command.register();
     * </pre>
     *
     * @return the character index in the input where completion starts
     * @since 1.6.5
     */
    public int getStart() {
        return base.getStart();
    }

    /**
     * the part of the input that is still being typed, from {@link #getStart()} onwards.
     * <p>
     * This is the text a suggestion is going to replace, and it is the thing to compare against
     * when deciding which suggestions to offer: a script wanting only the entries that start
     * with what has been typed reads this and filters on it. It is also what the
     * {@code suggest...} calls compare against, so a suggestion exactly equal to this is
     * quietly dropped.
     * <p>
     * The coordinate helpers on this class work in terms of this:
     * {@link #suggestPositions(String...)} and its siblings ask the game for suggestions that
     * fit the position typed after it.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("teleport")
     *   .literalArg("home")
     *   .suggest(JavaWrapper.methodToJava(function (ctx, suggestions) {
     *     const typed = suggestions.getRemaining();
     *     // only offer entries the player could still be typing towards
     *     for (const place of ["spawn", "base", "home"]) {
     *       if (place.startsWith(typed)) {
     *         suggestions.suggest(place);
     *       }
     *     }
     *   }));
     * command.register();
     * </pre>
     *
     * @return the unfinished tail of the input
     * @since 1.6.5
     */
    public String getRemaining() {
        return base.getRemaining();
    }

    /**
     * the same tail as {@link #getRemaining()}, lowercased.
     * <p>
     * Handy when the comparison should not care about case, since the game does not either when
     * it filters its own suggestions. It is a plain lowercasing of the tail, not a trim, so
     * leading spaces in the tail are kept.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("teleport")
     *   .literalArg("home")
     *   .suggest(JavaWrapper.methodToJava(function (ctx, suggestions) {
     *     // "SP", "Sp" and "sp" all compare equal to this
     *     const typed = suggestions.getRemainingLowerCase();
     *     for (const place of ["spawn", "base", "home"]) {
     *       if (place.startsWith(typed)) {
     *         suggestions.suggest(place);
     *       }
     *     }
     *   }));
     * command.register();
     * </pre>
     *
     * @return the unfinished tail of the input, lowercased
     * @since 1.6.5
     */
    public String getRemainingLowerCase() {
        return base.getRemainingLowerCase();
    }

    /**
     * adds a text suggestion to the list.
     * <p>
     * The suggestion is offered as a replacement for everything from {@link #getStart()} to the
     * end of the input, so what the player sees is the suggestion rather than what they typed
     * after the start index. Nothing is added if the suggestion is exactly what is already
     * typed, since that would change nothing and the client drops it anyway.
     * <br>
     * This returns the same helper, so calls chain.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("teleport")
     *   .literalArg("home")
     *   .suggest(JavaWrapper.methodToJava(function (ctx, suggestions) {
     *     // the first call replaces what is typed with "spawn", the rest add to the list
     *     suggestions.suggest("spawn").suggest("base").suggest("home");
     *   }));
     * command.register();
     * </pre>
     *
     * @param suggestion the text to offer
     * @return self for chaining
     * @since 1.6.5
     */
    public SuggestionsBuilderHelper suggest(String suggestion) {
        base.suggest(suggestion);
        return this;
    }

    /**
     * adds a number to the list as a suggestion.
     * <p>
     * The number is turned into text before it is added, so this is exactly
     * {@link #suggest(String)} with the number already written out: a text suggestion carrying
     * the digits, which replaces the tail from the start index like any other. The "already
     * typed" rule therefore applies here too, so offering a number the player has already typed
     * adds nothing. The only difference from the text form is that a script does not have to
     * convert the number itself.
     * <br>
     * This returns the same helper, so calls chain.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("setgamemode")
     *   .literalArg("value")
     *   .suggest(JavaWrapper.methodToJava(function (ctx, suggestions) {
     *     // each of these is added as a text suggestion carrying the digits
     *     suggestions.suggest(0).suggest(1).suggest(2).suggest(3);
     *   }));
     * command.register();
     * </pre>
     *
     * @param value the number to offer
     * @return self for chaining
     * @since 1.6.5
     */
    public SuggestionsBuilderHelper suggest(int value) {
        base.suggest(String.valueOf(value));
        return this;
    }

    /**
     * adds a text suggestion with a line of explanation beside it.
     * <p>
     * The tooltip is what the client shows next to the entry in the suggestion list when the
     * player holds the tooltip key, so this is where a script explains what a suggestion does.
     * It is a {@link TextHelper}, so it can be coloured and styled the same way any other text
     * is. Apart from the tooltip, this behaves exactly like {@link #suggest(String)}, including
     * the rule that a suggestion equal to what is already typed is not added.
     * <br>
     * This returns the same helper, so calls chain.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("teleport")
     *   .literalArg("home")
     *   .suggest(JavaWrapper.methodToJava(function (ctx, suggestions) {
     *     suggestions.suggestWithTooltip("spawn", Chat.createTextHelperFromString("world spawn point"));
     *     suggestions.suggestWithTooltip("base", Chat.createTextHelperFromString("your base, set with /sethome"));
     *   }));
     * command.register();
     * </pre>
     *
     * @param suggestion the text to offer
     * @param tooltip the text to show beside the suggestion
     * @return self for chaining
     * @since 1.6.5
     */
    public SuggestionsBuilderHelper suggestWithTooltip(String suggestion, TextHelper tooltip) {
        base.suggest(suggestion, tooltip.getRaw());
        return this;
    }

    /**
     * adds a number to the list as a suggestion, with a line of explanation beside it.
     * <p>
     * The number is turned into text before being added, so this is a text suggestion carrying
     * the digits, and the "already typed" rule of the text form applies here too. Where a script
     * wants a number with a tooltip beside it, this is the call for it since it is the only one
     * that takes both.
     * <br>
     * This returns the same helper, so calls chain.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("setgamemode")
     *   .literalArg("value")
     *   .suggest(JavaWrapper.methodToJava(function (ctx, suggestions) {
     *     const survival = Chat.createTextHelperFromString("take damage, need to eat");
     *     const creative = Chat.createTextHelperFromString("fly and break blocks");
     *     suggestions.suggestWithTooltip(0, survival).suggestWithTooltip(1, creative);
     *   }));
     * command.register();
     * </pre>
     *
     * @param value the number to offer
     * @param tooltip the text to show beside the suggestion
     * @return self for chaining
     * @since 1.6.5
     */
    public SuggestionsBuilderHelper suggestWithTooltip(int value, TextHelper tooltip) {
        base.suggest(String.valueOf(value), tooltip.getRaw());
        return this;
    }

    /**
     * offers a fixed list of strings.
     * <p>
     * This hands the list to the game's own matcher, which filters it against what the player
     * has typed and drops the entries that no longer fit, so a script can pass every option and
     * let the narrowing happen. The varargs form is the usual one; the collection form exists
     * for a list built at runtime.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("difficulty")
     *   .literalArg("value")
     *   .suggestMatching("peaceful", "easy", "normal", "hard");
     * command.register();
     * </pre>
     *
     * @param suggestions the strings to match
     * @return self for chaining.
     * @since 1.8.4
     */
    public SuggestionsBuilderHelper suggestMatching(String... suggestions) {
        return suggestMatching(Arrays.asList(suggestions));
    }

    /**
     * offers a fixed list of strings.
     * <p>
     * The collection form of {@link #suggestMatching(String...)}, for a list that is built
     * rather than written out.
     * example:
     * <pre>
     * const names = ["alpha", "beta", "gamma"];
     * const command = Chat.getCommandManager().createCommandBuilder("select")
     *   .literalArg("name")
     *   .suggest(JavaWrapper.methodToJava(function (ctx, suggestions) {
     *     suggestions.suggestMatching(names);
     *   }));
     * command.register();
     * </pre>
     *
     * @param suggestions the strings to match
     * @return self for chaining.
     * @since 1.8.4
     */
    public SuggestionsBuilderHelper suggestMatching(Collection<String> suggestions) {
        SharedSuggestionProvider.suggest(suggestions, base);
        return this;
    }

    /**
     * offers a fixed list of registry identifiers.
     * <p>
     * The same as {@link #suggestMatching(String...)} except each entry is parsed as a registry
     * identifier first, so a bad one is rejected outright rather than being offered and then
     * failing. Identifiers may leave the {@code minecraft:} namespace off.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("give")
     *   .literalArg("item")
     *   .suggestIdentifier("diamond", "emerald", "gold_ingot");
     * command.register();
     * </pre>
     *
     * @param identifiers the identifiers to match
     * @return self for chaining.
     * @since 1.8.4
     */
    public SuggestionsBuilderHelper suggestIdentifier(String... identifiers) {
        return suggestIdentifier(Arrays.asList(identifiers));
    }

    /**
     * offers a fixed list of registry identifiers.
     * <p>
     * The collection form of {@link #suggestIdentifier(String...)}, for a list that is built
     * rather than written out.
     * example:
     * <pre>
     * const blocks = ["stone", "dirt", "oak_log"];
     * const command = Chat.getCommandManager().createCommandBuilder("setblock")
     *   .literalArg("block")
     *   .suggest(JavaWrapper.methodToJava(function (ctx, suggestions) {
     *     suggestions.suggestIdentifier(blocks);
     *   }));
     * command.register();
     * </pre>
     *
     * @param identifiers the identifiers to match
     * @return self for chaining.
     * @since 1.8.4
     */
    public SuggestionsBuilderHelper suggestIdentifier(Collection<String> identifiers) {
        SharedSuggestionProvider.suggestResource(identifiers.stream().map(ResourceLocation::parse), base);
        return this;
    }

    /**
     * offers a fixed list of block positions.
     * <p>
     * Each position becomes the game's own coordinate suggestion, so the completion matches the
     * usual {@code x y z} form and understands the relative {@code ~} and {@code ^} forms. The
     * positions are written out as absolute numbers by this call, unlike
     * {@link #suggestPositions(java.util.Collection)} which passes its strings through, but
     * they are still narrowed the same way afterwards: the game's own coordinate matching is
     * what filters them against what the player has typed. A script that wants only nearby
     * positions therefore has to decide that itself before passing them.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("tp")
     *   .literalArg("pos")
     *   .suggestBlockPositions(
     *     PositionCommon.createBlockPos(0, 64, 0),
     *     PositionCommon.createBlockPos(100, 64, 100)
     *   );
     * command.register();
     * </pre>
     *
     * @param positions the positions to suggest
     * @return self for chaining.
     * @since 1.8.4
     */
    public SuggestionsBuilderHelper suggestBlockPositions(BlockPosHelper... positions) {
        return suggestPositions(Arrays.stream(positions).map(b -> b.getX() + " " + b.getY() + " " + b.getZ()).collect(Collectors.toList()));
    }

    /**
     * offers a fixed list of block positions.
     * <p>
     * The collection form of {@link #suggestBlockPositions(BlockPosHelper...)}, for a list that
     * is built rather than written out.
     * example:
     * <pre>
     * const spots = World.getEntities("minecraft:villager");
     * const command = Chat.getCommandManager().createCommandBuilder("tp")
     *   .literalArg("pos")
     *   .suggest(JavaWrapper.methodToJava(function (ctx, suggestions) {
     *     if (spots === null) {
     *       return;
     *     }
     *     for (const spot of spots) {
     *       suggestions.suggestPositions(spot.getX() + " " + spot.getY() + " " + spot.getZ());
     *     }
     *   }));
     * command.register();
     * </pre>
     *
     * @param positions the positions to suggest
     * @return self for chaining.
     * @since 1.8.4
     */
    public SuggestionsBuilderHelper suggestBlockPositions(Collection<BlockPosHelper> positions) {
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
    public SuggestionsBuilderHelper suggestPositions(String... positions) {
        return suggestPositions(Arrays.asList(positions));
    }

    /**
     * Positions are strings of the form "x y z" where x, y, and z are numbers or the default
     * minecraft selectors "~" and "^" followed by a number.
     * <p>
     * Unlike {@link #suggestBlockPositions(BlockPosHelper...)}, the strings go through as
     * written, so the relative {@code ~} and {@code ^} forms survive and the game's own
     * coordinate matching is what narrows them against what the player has typed.
     * <br>
     * Each string is split on single spaces and the first three parts are read straight out of
     * the result, so a string with fewer than three space-separated parts is not quietly
     * skipped: it throws an {@link java.lang.ArrayIndexOutOfBoundsException} while the
     * suggestions are being built. A double space makes an empty middle part rather than
     * closing the gap, so exactly one space between the three numbers is what is wanted.
     * example:
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("tp")
     *   .literalArg("pos")
     *   .suggestPositions("0 64 0", "~ ~ ~", "-100 ~ 100");
     * command.register();
     * </pre>
     *
     * @param positions the relative positions to suggest
     * @return self for chaining.
     * @since 1.8.4
     */
    public SuggestionsBuilderHelper suggestPositions(Collection<String> positions) {
        SharedSuggestionProvider.suggestCoordinates(getRemaining(), positions.stream().map(p -> {
            String[] split = p.split(" ");
            return new SharedSuggestionProvider.TextCoordinates(split[0], split[1], split[2]);
        }).collect(Collectors.toList()), base, s -> true);
        return this;
    }

}
