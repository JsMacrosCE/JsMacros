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
import com.jsmacrosce.jsmacros.client.api.helper.CommandNodeHelper;
import com.jsmacrosce.jsmacros.core.MethodWrapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @since 1.7.0
 */
public abstract class CommandManager {
    public static CommandManager instance;
    private static final Minecraft mc = Minecraft.getInstance();

    /**
     * @return list of commands
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
     * @param name
     * @return
     * @since 1.7.0
     */
    public abstract CommandBuilder createCommandBuilder(String name);

    /**
     * @param command
     * @return
     * @throws IllegalAccessException
     * @since 1.7.0
     */
    public abstract CommandNodeHelper unregisterCommand(String command) throws IllegalAccessException;

    /**
     * warning: this method is hacky
     *
     * @param node
     * @since 1.7.0
     */
    public abstract void reRegisterCommand(CommandNodeHelper node);

    /**
     * @param commandPart
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
