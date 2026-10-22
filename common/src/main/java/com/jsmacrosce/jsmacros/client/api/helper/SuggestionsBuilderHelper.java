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
 * Wraps Brigadier's builder for command-completion callbacks, exposing the input range and
 * methods for adding suggestions.
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
    /** Wraps the supplied Brigadier suggestion builder directly; it is retained, not copied.
     * Builder operations dereference it, so a {@code null} builder is not usable for those operations.
     *
     * @param base the builder to wrap; may be {@code null} and is retained as-is
     */
    public SuggestionsBuilderHelper(SuggestionsBuilder base) {
        super(base);
    }

    /**
     * Returns the full command input associated with this completion request.
     *
     * @return the full input line the suggestions are for
     * @since 1.6.5
     */
    public String getInput() {
        return base.getInput();
    }

    /**
     * Returns the start offset of the text being completed, measured in {@link #getInput()}.
     *
     * @return the character index in the input where completion starts
     * @since 1.6.5
     */
    public int getStart() {
        return base.getStart();
    }

    /**
     * Returns the input suffix beginning at {@link #getStart()}.
     *
     * @return the unfinished tail of the input
     * @since 1.6.5
     */
    public String getRemaining() {
        return base.getRemaining();
    }

    /**
     * Returns the input suffix beginning at {@link #getStart()}, lowercased without trimming
     * leading spaces.
     *
     * @return the unfinished tail of the input, lowercased
     * @since 1.6.5
     */
    public String getRemainingLowerCase() {
        return base.getRemainingLowerCase();
    }

    /**
     * Adds a text suggestion and returns this helper for chaining.
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
     * Adds the decimal string representation of a number as a suggestion and returns this helper.
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
     * Adds a text suggestion with a tooltip and returns this helper for chaining.
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
     * Adds the decimal string representation of a number with a tooltip and returns this helper
     * for chaining.
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
     * Offers string suggestions filtered against the text currently being completed.
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("difficulty")
     *   .literalArg("value")
     *   .suggestMatching("peaceful", "easy", "normal", "hard");
     * command.register();
     * </pre>
     *
     * @param suggestions the strings to match
     * @return self for chaining
     * @since 1.8.4
     */
    public SuggestionsBuilderHelper suggestMatching(String... suggestions) {
        return suggestMatching(Arrays.asList(suggestions));
    }

    /**
     * Offers string suggestions filtered against the text currently being completed.
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
     * @return self for chaining
     * @since 1.8.4
     */
    public SuggestionsBuilderHelper suggestMatching(Collection<String> suggestions) {
        SharedSuggestionProvider.suggest(suggestions, base);
        return this;
    }

    /**
     * Offers registry identifier suggestions, parsing each string as a resource location before
     * passing it to the game's resource matcher.
     * <pre>
     * const command = Chat.getCommandManager().createCommandBuilder("give")
     *   .literalArg("item")
     *   .suggestIdentifier("diamond", "emerald", "gold_ingot");
     * command.register();
     * </pre>
     *
     * @param identifiers the identifiers to match
     * @return self for chaining
     * @since 1.8.4
     */
    public SuggestionsBuilderHelper suggestIdentifier(String... identifiers) {
        return suggestIdentifier(Arrays.asList(identifiers));
    }

    /**
     * Offers registry identifier suggestions from a collection, parsing each string as a resource
     * location before passing it to the game's resource matcher.
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
     * @return self for chaining
     * @since 1.8.4
     */
    public SuggestionsBuilderHelper suggestIdentifier(Collection<String> identifiers) {
        SharedSuggestionProvider.suggestResource(identifiers.stream().map(ResourceLocation::parse), base);
        return this;
    }

    /**
     * Offers block positions as {@code x y z} coordinate suggestions.
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
     * @return self for chaining
     * @since 1.8.4
     */
    public SuggestionsBuilderHelper suggestBlockPositions(BlockPosHelper... positions) {
        return suggestPositions(Arrays.stream(positions).map(b -> b.getX() + " " + b.getY() + " " + b.getZ()).collect(Collectors.toList()));
    }

    /**
     * Offers the block positions in a collection as {@code x y z} coordinate suggestions.
     *
     * @param positions the positions to suggest
     * @return self for chaining
     * @since 1.8.4
     */
    public SuggestionsBuilderHelper suggestBlockPositions(Collection<BlockPosHelper> positions) {
        return suggestPositions(positions.stream().map(b -> b.getX() + " " + b.getY() + " " + b.getZ()).collect(Collectors.toList()));
    }

    /**
     * Offers coordinate strings containing two or three whitespace-separated components; all
     * strings in a call must have the same number of components. Strings are passed to the
     * collection overload for validation and suggestion.
     *
     * @param positions the positions to suggest
     * @throws IllegalArgumentException if a position has neither two nor three coordinates,
     * or if the positions do not all have the same number of coordinates
     * @return self for chaining.
     * @since 1.8.4
     */
    public SuggestionsBuilderHelper suggestPositions(String... positions) {
        return suggestPositions(Arrays.asList(positions));
    }

    /**
     * Validates coordinate strings containing two or three whitespace-separated components, with
     * the same component count throughout the collection, then offers them through the game's
     * 2D or 3D coordinate matcher against the current unfinished input. Components are split on
     * whitespace (surrounding and repeated spaces are not retained); relative-coordinate tokens
     * are passed to the matcher. An empty collection adds no suggestions and a {@code null}
     * collection is not accepted.
     *
     * @param positions the coordinate strings to suggest
     * @throws IllegalArgumentException if a position has neither two nor three coordinates,
     * or if the positions do not all have the same number of coordinates
     * @return self for chaining
     * @since 1.8.4
     */
    public SuggestionsBuilderHelper suggestPositions(Collection<String> positions) {
        var coordinates = positions.stream().map(p -> p.trim().split("\\s+"))
                .peek(parts -> {
                    if (parts.length != 2 && parts.length != 3) {
                        throw new IllegalArgumentException("A position must have two or three coordinates");
                    }
                }).toList();
        if (!coordinates.isEmpty()) {
            int dimensions = coordinates.getFirst().length;
            if (coordinates.stream().anyMatch(parts -> parts.length != dimensions)) {
                throw new IllegalArgumentException("Positions must all have the same number of coordinates");
            }
            var values = coordinates.stream().map(parts -> new SharedSuggestionProvider.TextCoordinates(
                    parts[0], dimensions == 3 ? parts[1] : "", parts[dimensions - 1])).toList();
            if (dimensions == 2) {
                SharedSuggestionProvider.suggest2DCoordinates(getRemaining(), values, base, s -> true);
            } else {
                SharedSuggestionProvider.suggestCoordinates(getRemaining(), values, base, s -> true);
            }
        }
        return this;
    }

}
