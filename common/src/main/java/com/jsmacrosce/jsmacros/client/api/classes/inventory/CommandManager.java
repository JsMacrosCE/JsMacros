package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.doclet.DocletIgnore;
import com.jsmacrosce.jsmacros.client.api.helper.CommandNodeHelper;
import com.jsmacrosce.jsmacros.core.MethodWrapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Provides access to the client's command tree, command builders, and command autocomplete.
 * Obtain the shared instance with {@code Chat.getCommandManager()}.
 * <pre>
 * // list the commands in the current client command tree
 * for (const name of Chat.getCommandManager().getValidCommands()) {
 *   Chat.log(name);
 * }
 * // register one of our own
 * const command = Chat.getCommandManager().createCommandBuilder("hello");
 * command.literalArg("world")
 * .executes(JavaWrapper.methodToJava(function (ctx) {
 *   Chat.log("hello world");
 *   return true;
 * }));
 * command.register();
 * </pre>
 * @since 1.7.0
 */
@DocletCategory("Commands")
public abstract class CommandManager {
    /**
     * Shared command manager installed by the loader. Scripts should obtain it through
     * {@code Chat.getCommandManager()}.
     * @since 1.7.0
     */
    @DocletIgnore
    public static CommandManager instance;
    private static final Minecraft mc = Minecraft.getInstance();

    /**
     * @return names of the root command tree's direct children; empty when no connection is available
     * @since 1.7.0
     */
    public List<String> getValidCommands() {
        ClientPacketListener nh = Minecraft.getInstance().getConnection();
        if (nh == null) {
            return ImmutableList.of();
        }
        return nh.getCommands().getRoot().getChildren().stream().map(CommandNode::getName).collect(Collectors.toList());
    }

    /**
     * Creates a builder for a client command; it is not registered until {@link CommandBuilder#register()} is called.
     * @param name command name, without a leading slash
     * @return a builder initialized for that command
     * @since 1.7.0
     */
    public abstract CommandBuilder createCommandBuilder(String name);

    /**
     * Removes a named command from the client command dispatchers.
     * @param command name of the command to remove
     * @return a helper for the removed command node, for use with {@link #reRegisterCommand(CommandNodeHelper)}
     * @throws IllegalAccessException if the command cannot be reached for removal
     * @since 1.7.0
     */
    public abstract CommandNodeHelper unregisterCommand(String command) throws IllegalAccessException;

    /**
     * Re-registers a command node previously removed with {@link #unregisterCommand(String)}.
     * @param node helper returned by {@link #unregisterCommand(String)}
     * @since 1.7.0
     */
    public abstract void reRegisterCommand(CommandNodeHelper node);

    /**
     * Requests autocomplete suggestions and passes their text values to the callback when the
     * asynchronous request completes. Requires a player to be present.
     * @param commandPart partial command text to complete
     * @param callback receives the list of suggested completion text when the request completes
     * @since 1.8.2
     */
    public void getArgumentAutocompleteOptions(String commandPart, MethodWrapper<List<String>, Object, Object, ?> callback) {
        assert mc.player != null;
        //? if >1.21.5 {
        CommandDispatcher<ClientSuggestionProvider> commandDispatcher = mc.player.connection.getCommands();
        ParseResults<ClientSuggestionProvider> parse = commandDispatcher.parse(commandPart, mc.player.connection.getSuggestionsProvider());
        //?} else {
        /*CommandDispatcher<SharedSuggestionProvider> commandDispatcher = mc.player.connection.getCommands();
        ParseResults<SharedSuggestionProvider> parse = commandDispatcher.parse(commandPart, mc.player.connection.getSuggestionsProvider());
        *///?}

        CompletableFuture<Suggestions> suggestions = commandDispatcher.getCompletionSuggestions(parse);
        suggestions.thenAccept(
                (s) -> {
                    List<String> list = s.getList().stream().map(Suggestion::getText).collect(Collectors.toList());
                    callback.accept(list);
                }
        );
    }

}
