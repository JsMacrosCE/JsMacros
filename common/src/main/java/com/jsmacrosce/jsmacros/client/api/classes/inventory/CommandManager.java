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
import com.jsmacrosce.jsmacros.client.api.helper.CommandNodeHelper;
import com.jsmacrosce.jsmacros.core.MethodWrapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * the client side command registry, reached through
 * {@code Chat.getCommandManager()}.
 * <p>
 * It is the entry point for the two halves of the command system: reading the commands that
 * exist, and building new ones. It holds no state of its own for either. {@link
 * #getValidCommands()} reads the live command tree off the connection, and the builders handed
 * out by {@link #createCommandBuilder(String)} keep their pending commands in a map on the
 * loader's {@code CommandBuilder} subclass, so a script cannot read either back out of the
 * manager.
 * <p>
 * The class is abstract, so the object behind it is whichever one the loader put in place
 * during startup, and that is also why {@link #instance} is a field rather than something a
 * script constructs: there is one of these, not one per script.
 * <p>
 * The list from {@link #getValidCommands()} is the client view of the command tree, so it
 * holds the vanilla commands the connected server advertised as well as anything a script
 * has registered. It is empty when nothing is connected, which is worth handling rather than
 * assuming.
 * example:
 * <pre>
 * // every command the client currently knows about
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
     * the one command manager this client has.
     * <p>
     * It is set during startup by the loader and is not something a script should assign,
     * since replacing it would leave the rest of the client still holding the old one. Reach
     * for {@code Chat.getCommandManager()} rather than reading this directly, which gives the
     * same object and keeps working if the field is ever made private.
     * example:
     * <pre>
     * // the same object Chat.getCommandManager() returns
     * const manager = Java.type("com.jsmacrosce.jsmacros.client.api.classes.inventory.CommandManager")
     * .instance;
     * Chat.log(`${manager.getValidCommands().size()} commands`);
     * </pre>
     * @since 1.7.0
     */
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
