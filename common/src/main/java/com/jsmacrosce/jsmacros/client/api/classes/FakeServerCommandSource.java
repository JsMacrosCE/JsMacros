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
 * A stand in for a server side command source, built on the client.
 * <p>
 * A command on a client still runs through the game's own command machinery, and that machinery
 * asks its source where the player is, who is online, which registries exist and so on. This is the
 * source such a command is given: it extends the game's ordinary source but has no server behind
 * it, so every call that would have asked a server is answered from what the client already knows
 * instead. That is what makes an entity selector, a position argument and a dimension suggestion
 * work in a client command at all, since without it each of those would be looking for a server
 * that is not there.
 * <p>
 * It is the client's own suggestion provider doing the answering, and a fresh one is built for each
 * argument a command reads, so there is nothing for a script to keep alive. A script never
 * constructs one: it meets one as the source of a command it registered, and everything here is
 * reached from that.
 * <p>
 * <b>This class is not in the shipped TypeScript definitions and a script never names it.</b> The
 * route in is the source of a client command's own context, whose raw form carries these calls.
 * example:
 * <pre>
 * // a client command, whose source is this fake one rather than a real
 * // server side source
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
     * builds the fake source for a command running on the client.
     * <p>
     * The player is the one the command is being run for, and the source is the client's own
     * suggestion provider, which is what every call below is answered from. The ordinary source is
     * given no server and no level, which is exactly what those overrides exist to work around,
     * along with a full permission level so the command is not turned away on that count.
     * <p>
     * A script never builds one of these. It meets one as the source of a command it registered.
     * example:
     * <pre>
     * // a script never constructs this, it meets one as the source of a
     * // command it registered, and reads what that source answers off
     * // the context's raw form
     * const builder = Chat.getCommandManager().createCommandBuilder("probe");
     * builder.executes(JavaWrapper.methodToJava(function (ctx) {
     *   const src = ctx.getRaw().getSource();
     *   Chat.log(`running as ${src.getTextName()}`);
     *   return true;
     * }));
     * builder.register();
     * </pre>
     *
     * @param source the client's suggestion provider, which every overridden call is answered from
     * @param player the player the command is being run for
     * @throws NullPointerException if {@code player} is {@code null}, since its position and name
     *         are read to build the source
     * @since 1.8.4
     */
    public FakeServerCommandSource(ClientSuggestionProvider source, LocalPlayer player) {
        // TODO: (1.21.11) These are specified as NotNull, will this fail?
        //? if >=1.21.11 {
        /*super(null, player.position(), player.getRotationVector(), null, PermissionSet.ALL_PERMISSIONS, player.getName().getString(), player.getDisplayName(), null, player);
        *///? } else {
        super(null, player.position(), player.getRotationVector(), null, 100, player.getName().getString(), player.getDisplayName(), null, player);
        //? }
        this.source = source;
    }

    /**
     * Without this the entity selector would have nothing to offer, because the ordinary source
     * answers it with an empty collection and a client command has no other way to name an entity.
     * <p>
     * The client's answer is the entity under the crosshair, as its string UUID in a collection of
     * at most one. The crosshair being on a block, on nothing, or absent altogether all give the
     * empty collection rather than a name, so the presence of an entity here means the player is
     * looking straight at one.
     * example:
     * <pre>
     * const builder = Chat.getCommandManager().createCommandBuilder("probe");
     * builder.intArg("count", 1, 64);
     * builder.executes(JavaWrapper.methodToJava(function (ctx) {
     *   // the source of a client command is this fake one rather than a
     *   // real server side source
     *   const src = ctx.getRaw().getSource();
     *   const selected = src.getSelectedEntities();
     *   Chat.log(`${selected.size()} selected`);
     *   return true;
     * }));
     * builder.register();
     * </pre>
     *
     * @return the entity under the crosshair as its id, in a collection of at most one, or an
     *         empty collection when the crosshair is not on an entity
     * @since 1.8.4
     */
    @Override
    public Collection<String> getSelectedEntities() {
        return source.getSelectedEntities();
    }


    /**
     * The ordinary source answers this with the list of player names, because the only form of the
     * call the base class carries is the three 'g' spelling and that one defaults to the online
     * players.
     * <p>
     * The client's answer is the custom tab completions the server has sent, with the player names
     * added on top of them. While the server has sent no custom completions at all it is the player
     * names alone, so the two are not separable from here: a name in this list is a player, and
     * anything else is something the server has advertised for tab completion.
     * example:
     * <pre>
     * const builder = Chat.getCommandManager().createCommandBuilder("probe");
     * builder.intArg("count", 1, 64);
     * builder.executes(JavaWrapper.methodToJava(function (ctx) {
     *   // the source of a client command is this fake one rather than a
     *   // real server side source
     *   const src = ctx.getRaw().getSource();
     *   const tab = src.getCustomTabSuggestions();
     *   for (const name of tab) {
     *     Chat.log(`tab completion offers ${name}`);
     *   }
     *   return true;
     * }));
     * builder.register();
     * </pre>
     *
     * @return the tab completions the client has been sent, which is whatever the connected
     *         server last sent rather than a fresh query
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
     * This is the three 'g' spelling, which is a leftover misspelling of the same call the game
     * still carries. It is marked deprecated, it is left out of the shipped TypeScript definitions,
     * and all it does is write a warning to the standard error stream and then do what
     * {@link #getCustomTabSuggestions()} does.
     * <p>
     * A script that somehow reaches it therefore gets the same answer as the correct spelling and
     * a line in the log it did not ask for. There is no behaviour to choose between and no reason
     * to prefer it.
     * example:
     * <pre>
     * const builder = Chat.getCommandManager().createCommandBuilder("probe");
     * builder.intArg("count", 1, 64);
     * builder.executes(JavaWrapper.methodToJava(function (ctx) {
     *   // the source of a client command is this fake one rather than a
     *   // real server side source
     *   const src = ctx.getRaw().getSource();
     *   // the deprecated three g spelling still answers, with a warning
     *   // on the error stream
     *   const tab = src.getCustomTabSuggestions();
     *   Chat.log(`${tab.size()} tab suggestions`);
     *   return true;
     * }));
     * builder.register();
     * </pre>
     *
     * @return the same thing as {@link #getCustomTabSuggestions()}, since that is all this does
     *         after printing a warning
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
     * The ordinary source answers this by asking the server it is built on for the names on its
     * player list. There is no such server behind a client command, which is why this is overridden
     * rather than left alone.
     * <p>
     * The client's answer comes from its own connection instead, which is the tab list the server
     * has already sent down. A name is therefore here from the moment the server puts it in the tab
     * list rather than from a query, and a name the server has not sent is not here however real it
     * is.
     * example:
     * <pre>
     * const builder = Chat.getCommandManager().createCommandBuilder("probe");
     * builder.intArg("count", 1, 64);
     * builder.executes(JavaWrapper.methodToJava(function (ctx) {
     *   // the source of a client command is this fake one rather than a
     *   // real server side source
     *   const src = ctx.getRaw().getSource();
     *   const names = src.getOnlinePlayerNames();
     *   Chat.log(`${names.size()} players on this server`);
     *   return true;
     * }));
     * builder.register();
     * </pre>
     *
     * @return the names of the players the client knows about, read from its own tab list
     * @since 1.8.4
     */
    @Override
    public Collection<String> getOnlinePlayerNames() {
        return source.getOnlinePlayerNames();
    }

    /**
     * The ordinary source answers this by asking the server's scoreboard for the teams on it, and a
     * client command has no scoreboard of its own to ask.
     * <p>
     * The client's answer comes from the client's own copy of the scoreboard, which is the one the
     * server has already sent. A team is here as soon as the server has told the client about it,
     * so this reflects the scoreboard as the server last had it rather than a fresh reading of it.
     * example:
     * <pre>
     * const builder = Chat.getCommandManager().createCommandBuilder("probe");
     * builder.intArg("count", 1, 64);
     * builder.executes(JavaWrapper.methodToJava(function (ctx) {
     *   // the source of a client command is this fake one rather than a
     *   // real server side source
     *   const src = ctx.getRaw().getSource();
     *   const teams = src.getAllTeams();
     *   Chat.log(`${teams.size()} teams known to the client`);
     *   return true;
     * }));
     * builder.register();
     * </pre>
     *
     * @return the team names on the client's own scoreboard, which the server has already sent
     * @since 1.8.4
     */
    @Override
    public Collection<String> getAllTeams() {
        return source.getAllTeams();
    }

    /**
     * The ordinary source answers this with every sound event in the game's own built in registry,
     * mapped to its id, so a sound that only a data pack adds is not among them.
     * <p>
     * The client's answer comes from its sound manager, which holds the sounds the server has
     * actually sent. That is the shorter of the two lists, and it is the one that matches what the
     * player can hear, so a sound the server has not sent does not appear here even though the game
     * knows about it.
     * example:
     * <pre>
     * const builder = Chat.getCommandManager().createCommandBuilder("probe");
     * builder.intArg("count", 1, 64);
     * builder.executes(JavaWrapper.methodToJava(function (ctx) {
     *   // the source of a client command is this fake one rather than a
     *   // real server side source
     *   const src = ctx.getRaw().getSource();
     *   const sounds = src.getAvailableSounds();
     *   Chat.log(`${sounds.count()} sounds the server has sent`);
     *   return true;
     * }));
     * builder.register();
     * </pre>
     *
     * @return a stream of the sound ids the client's sound manager holds, which is what the
     *         server has sent rather than every sound the game knows about
     * @since 1.8.4
     */
    @Override
    public Stream<ResourceLocation> getAvailableSounds() {
        return source.getAvailableSounds();
    }

    /**
     * The ordinary source answers this with nothing at all, since it has nowhere to send a query.
     * <p>
     * A client command does have somewhere to send one, so this hands the whole command line to the
     * server and returns the future the answer will arrive on. The suggestions are the server's own
     * rather than the client's, which is the whole point of the override, and the future is not done
     * when this returns.
     * example:
     * <pre>
     * const builder = Chat.getCommandManager().createCommandBuilder("probe");
     * builder.intArg("count", 1, 64);
     * builder.executes(JavaWrapper.methodToJava(function (ctx) {
     *   // the source of a client command is this fake one rather than a
     *   // real server side source
     *   const src = ctx.getRaw().getSource();
     *   // the future is not done when this returns, and asking again
     *   // cancels the request before it
     *   const pending = src.customSuggestion(ctx.getRaw());
     *   pending.thenAccept(function (suggestions) {
     *     Chat.log(`${suggestions.getList().size()} suggestions came back`);
     *   });
     *   return true;
     * }));
     * builder.register();
     * </pre>
     *
     * @param context the command context the suggestions are for, handed to the client as it is
     * @return a future of the suggestions, which is not done when this returns and which a
     *         second request from the same client cancels
     * @since 1.8.4
     */
    @Override
    public CompletableFuture<Suggestions> customSuggestion(CommandContext<?> context) {
        return source.customSuggestion(context);
    }

    /**
     * This is what a coordinate suggestion is built from. The ordinary source has a single form of
     * three tildes, which is the running position and nothing else.
     * <p>
     * The client's answer replaces that with the position of the block under the crosshair, written
     * out as three whole numbers, whenever the crosshair is on a block. The crosshair being on
     * anything else, or absent, falls back to the same single form of three tildes, so the
     * difference is only ever a better suggestion and never an empty one.
     * example:
     * <pre>
     * const builder = Chat.getCommandManager().createCommandBuilder("probe");
     * builder.intArg("count", 1, 64);
     * builder.executes(JavaWrapper.methodToJava(function (ctx) {
     *   // the source of a client command is this fake one rather than a
     *   // real server side source
     *   const src = ctx.getRaw().getSource();
     *   const forms = src.getRelevantCoordinates();
     *   Chat.log(`${forms.size()} coordinate forms to suggest`);
     *   return true;
     * }));
     * builder.register();
     * </pre>
     *
     * @return the coordinate forms a suggestion should offer, from the client rather than the
     *         single general entry the ordinary source gives
     * @since 1.8.4
     */
    @Override
    public Collection<TextCoordinates> getRelevantCoordinates() {
        return source.getRelevantCoordinates();
    }

    /**
     * This is the absolute counterpart of {@link #getRelevantCoordinates()}, and the two differ in a
     * detail worth knowing: this one uses the crosshair's own point rather than the block it is in,
     * so its coordinates are the fractional ones and that one's are whole numbers.
     * <p>
     * Both fall back to a single form of three tildes when the crosshair is not on a block, and
     * both return exactly one form when they do not fall back.
     * example:
     * <pre>
     * const builder = Chat.getCommandManager().createCommandBuilder("probe");
     * builder.intArg("count", 1, 64);
     * builder.executes(JavaWrapper.methodToJava(function (ctx) {
     *   // the source of a client command is this fake one rather than a
     *   // real server side source
     *   const src = ctx.getRaw().getSource();
     *   const forms = src.getAbsoluteCoordinates();
     *   Chat.log(`${forms.size} absolute coordinate forms to suggest`);
     *   return true;
     * }));
     * builder.register();
     * </pre>
     *
     * @return the absolute coordinate forms a suggestion should offer, from the client rather
     *         than the single general entry the ordinary source gives
     * @since 1.8.4
     */
    @Override
    public Collection<TextCoordinates> getAbsoluteCoordinates() {
        return source.getAbsoluteCoordinates();
    }

    /**
     * The ordinary source answers this by asking the server for the dimension keys it has
     * loaded, which is a question only a server can answer.
     * <p>
     * The client's answer comes from its own connection instead, which is the list the server sent
     * when the world was joined. A dimension argument is therefore completed from what the client
     * has been told rather than from a query, so a dimension the server has not announced does not
     * appear here.
     * example:
     * <pre>
     * const builder = Chat.getCommandManager().createCommandBuilder("probe");
     * builder.intArg("count", 1, 64);
     * builder.executes(JavaWrapper.methodToJava(function (ctx) {
     *   // the source of a client command is this fake one rather than a
     *   // real server side source
     *   const src = ctx.getRaw().getSource();
     *   const levels = src.levels();
     *   Chat.log(`${levels.size()} dimensions the client knows`);
     *   return true;
     * }));
     * builder.register();
     * </pre>
     *
     * @return the dimension keys the client has been told about, which is what a dimension
     *         argument's suggestions are built from
     * @since 1.8.4
     */
    @Override
    public Set<ResourceKey<Level>> levels() {
        return source.levels();
    }

    /**
     * The ordinary source answers this by asking the server for the registries the server is
     * running, which is the one thing a client cannot have.
     * <p>
     * The client's answer comes from its own connection instead, which is the set the server has
     * sent down. An argument that has to look something up therefore sees the server's own
     * registries, including anything its data packs have added, rather than only the set the game
     * ships with.
     * example:
     * <pre>
     * const builder = Chat.getCommandManager().createCommandBuilder("probe");
     * builder.intArg("count", 1, 64);
     * builder.executes(JavaWrapper.methodToJava(function (ctx) {
     *   // the source of a client command is this fake one rather than a
     *   // real server side source
     *   const src = ctx.getRaw().getSource();
     *   // this is what makes a lookup inside an argument work, since the
     *   // entity selector and the position argument both need it
     *   const access = src.registryAccess();
     *   Chat.log(`registry access present: ${access !== null}`);
     *   return true;
     * }));
     * builder.register();
     * </pre>
     *
     * @return the client's own registry access, which is what an argument that has to look
     *         something up reads
     * @since 1.8.4
     */
    @Override
    public RegistryAccess registryAccess() {
        return source.registryAccess();
    }

    /**
     * The ordinary source sends this to the player and, when asked, relays it to the
     * administrators as well. Both of those are subject to flags on the source itself, so a source
     * that does not want success output shows nothing and one that is not broadcasting does not
     * reach the operators.
     * <p>
     * There is no second destination here and the flag is not read. The text goes to the player's
     * chat once and nowhere else, so a command whose feedback was meant to be broadcast reaches
     * only the player who ran it, and the supplier is asked for exactly once.
     * example:
     * <pre>
     * const builder = Chat.getCommandManager().createCommandBuilder("probe");
     * builder.intArg("count", 1, 64);
     * builder.executes(JavaWrapper.methodToJava(function (ctx) {
     *   // the source of a client command is this fake one rather than a
     *   // real server side source
     *   const src = ctx.getRaw().getSource();
     *   // the flag is ignored here: the text goes to the player's chat
     *   // once and nowhere else
     *   src.sendSuccess(JavaWrapper.methodToJava(function () {
     *     return Chat.createTextBuilder().append("probe ran").withColor(0x6).build();
     *   }), false);
     *   return true;
     * }));
     * builder.register();
     * </pre>
     *
     * @param feedbackSupplier supplies the text to show, and is only asked for when something is
     *                        going to be shown
     * @param broadcastToOps ignored; there are no operators to broadcast to on a client
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
