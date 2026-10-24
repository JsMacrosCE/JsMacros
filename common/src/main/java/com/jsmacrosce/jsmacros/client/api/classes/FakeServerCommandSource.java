package com.jsmacrosce.jsmacros.client.api.classes;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import com.jsmacrosce.doclet.DocletIgnore;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;

//? if >=1.21.11 {
/*import net.minecraft.server.permissions.PermissionSet;
*///? }

/**
 * A client-side command source that delegates suggestion and connection data to the client's
 * {@link ClientSuggestionProvider}. Scripts encounter it as the source in a client command's raw
 * context rather than constructing it directly.
 * <pre>
 * // Read the client-side command source through the raw context.
 * const builder = Chat.getCommandManager().createCommandBuilder("probe");
 * builder.intArg("count", 1, 64);
 * builder.executes(JavaWrapper.methodToJava(function (ctx) {
 *   const src = ctx.getRaw().getSource();
 *   const players = src.getOnlinePlayerNames();
 *   Chat.log(`${players.size()} players online`);
 *   return true;
 * }));
 * builder.register();
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
public class FakeServerCommandSource extends CommandSourceStack {

    private final ClientSuggestionProvider source;

    /**
     * Builds a client-side command source using the supplied player and suggestion provider.
     *
     * @param source the client provider used to answer suggestion and connection-data queries
     * @param player the player the command is being run for
     * @throws NullPointerException if {@code player} is {@code null}, since its position and name
     *         are read to build the source
     * @since 1.8.4
     */
    public FakeServerCommandSource(ClientSuggestionProvider source, LocalPlayer player) {
        // TODO: (1.21.11) These are specified as NotNull, will this fail?
        //? if >=26.3 {
        /*super(null, player.position(), player.getRotationVector(), null, PermissionSet.ALL_PERMISSIONS, null, player);
        *///? } else if >=1.21.11 {
        /*super(null, player.position(), player.getRotationVector(), null, PermissionSet.ALL_PERMISSIONS, player.getName().getString(), player.getDisplayName(), null, player);
        *///? } else {
        super(null, player.position(), player.getRotationVector(), null, 100, player.getName().getString(), player.getDisplayName(), null, player);
        //? }
        this.source = source;
    }

    /**
     * Returns the UUID string of the entity under the client's crosshair, if the current hit is an entity.
     *
     * @return the entity's UUID string, in a collection of at most one, or an
     *         empty collection otherwise
     * @since 1.8.4
     */
    @Override
    public Collection<String> getSelectedEntities() {
        return source.getSelectedEntities();
    }


    /**
     * Returns server-provided custom completions together with online player names. If no custom
     * completions are present, the result contains the online player names alone.
     *
     * @return custom completions and online player names from the client provider
     * @since 1.8.4
     */
    //? if >=26.1 {
    /*@Override
    *///? }
    public Collection<String> getCustomTabSuggestions() {
        //? if >=26.1 {
        /*return source.getCustomTabSuggestions();
        *///? } else {
        return source.getCustomTabSugggestions();
        //? }
    }

    /**
     * Deprecated misspelling of {@link #getCustomTabSuggestions()}; writes a warning to standard
     * error, then returns the correctly-spelled method's result.
     *
     * @return the result of {@link #getCustomTabSuggestions()}
     * @since 1.8.4
     */
    //? if <26.1 {
    @Override
    //? }
    @Deprecated
    @DocletIgnore
    public Collection<String> getCustomTabSugggestions() {
        // TODO: Implement proper deprecation system
        System.err.println("Warning: getCustomTabSugggestions() is deprecated and will be removed in the future. Use getCustomTabSuggestions() instead.");
        return this.getCustomTabSuggestions();
    }

    /**
     * Returns player names currently present in the client's connection player list.
     *
     * @return names in the client's current player list
     * @since 1.8.4
     */
    @Override
    public Collection<String> getOnlinePlayerNames() {
        return source.getOnlinePlayerNames();
    }

    /**
     * Returns the team names in the scoreboard held by the client's connection.
     *
     * @return the team names in the client's scoreboard
     * @since 1.8.4
     */
    @Override
    public Collection<String> getAllTeams() {
        return source.getAllTeams();
    }

    /**
     * Returns sound ids available through the client's sound manager.
     *
     * @return a stream of sound ids available through the client's sound manager
     * @since 1.8.4
     */
    @Override
    public Stream<ResourceLocation> getAvailableSounds() {
        return source.getAvailableSounds();
    }

    /**
     * Requests completions for the command input from the server. A new request
     * cancels the previous pending request from this provider.
     *
     * @param context command context whose input is sent for completion
     * @return the future completed with the server's response; a subsequent request cancels it
     * @since 1.8.4
     */
    @Override
    public CompletableFuture<Suggestions> customSuggestion(CommandContext<?> context) {
        return source.customSuggestion(context);
    }

    /**
     * Supplies the block position under the crosshair for coordinate suggestions,
     * or the default three tildes when the crosshair is not on a block.
     *
     * @return one block-position coordinate form, or the default three-tildes form
     * @since 1.8.4
     */
    @Override
    public Collection<TextCoordinates> getRelevantCoordinates() {
        return source.getRelevantCoordinates();
    }

    /**
     * Supplies the crosshair hit point for absolute coordinate suggestions, formatted
     * to two decimal places, or the default three tildes when it is not on a block.
     * Unlike {@link #getRelevantCoordinates()}, this uses the hit point rather than
     * the block position.
     *
     * @return one hit-point coordinate form, or the default three-tildes form
     * @since 1.8.4
     */
    @Override
    public Collection<TextCoordinates> getAbsoluteCoordinates() {
        return source.getAbsoluteCoordinates();
    }

    /**
     * Returns dimension keys available on the client's current connection.
     *
     * @return the dimension keys available on the client's connection
     * @since 1.8.4
     */
    @Override
    public Set<ResourceKey<Level>> levels() {
        return source.levels();
    }

    /**
     * Returns registry access from the client's current connection, including
     * registries synchronized by the server.
     *
     * @return registry access from the client's connection
     * @since 1.8.4
     */
    @Override
    public RegistryAccess registryAccess() {
        return source.registryAccess();
    }

    /**
     * Sends the supplied feedback to the local player's chat. The broadcast flag is ignored, and
     * the supplier is evaluated once.
     *
     * @param feedbackSupplier supplies the feedback component
     * @param broadcastToOps ignored
     * @since 1.8.4
     */
    @Override
    public void sendSuccess(Supplier<Component> feedbackSupplier, boolean broadcastToOps) {
        //? if >=26.1 {
        /*Minecraft.getInstance().player.sendSystemMessage(feedbackSupplier.get());
        *///? } else {
        Minecraft.getInstance().player.displayClientMessage(feedbackSupplier.get(), false);
        //? }
    }

}
