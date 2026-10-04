package com.jsmacrosce.jsmacros.client.api.helper;

import com.jsmacrosce.doclet.DocletCategory;
import com.mojang.brigadier.tree.CommandNode;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * a command node that has been taken out of the command tree, so it can be put back.
 * <p>
 * A script registers a command through the command manager and the node it ends up as is what
 * {@code unregisterCommand} hands back and {@code reRegisterCommand} takes again. This wrapper
 * exists to hold that node between the two calls, and it is the only thing the class adds: a
 * Brigadier command node and nothing of its own, so everything interesting about it is
 * {@code getRaw()} and {@link #fabric}.
 * <p>
 * A name can be registered in <b>two</b> places, and that is what the two fields are for. The
 * node a server sends down lives in the connection's own command dispatcher and is what
 * {@code getRaw()} returns; the node a mod's client-side command API registers lives in that
 * API's dispatcher and is what {@link #fabric} returns. A name may be in either, both, or
 * neither, so <b>both fields can be {@code null}</b>: neither is ever guaranteed, and a script
 * that reads one has to check it. Putting a command back works either way, because the manager
 * only puts back the fields that are set.
 * <p>
 * There is a third way this object gets made. The runtime wraps any raw Brigadier
 * {@code CommandNode} a script-facing call returns, and that path goes through the one-argument
 * constructor, which has no client-side dispatcher to look in and so always leaves
 * {@link #fabric} at {@code null}. A helper obtained that way therefore has the server node in
 * {@code getRaw()} and nothing in {@link #fabric}.
 * <br>
 * Nothing in the game shows this; it is a book-keeping handle for a script that is managing its
 * own commands.
 * example:
 * <pre>
 * const manager = Chat.getCommandManager();
 *
 * // take a command out, which gives back the node it was built from
 * const node = manager.unregisterCommand("hello");
 * if (node !== null) {
 *   // getRaw is the server's copy and fabric is the client-side copy, and
 *   // either can be null when only one of the two had the name
 *   if (node.getRaw() !== null) {
 *     Chat.log("was a server command");
 *   }
 *   if (node.fabric !== null) {
 *     Chat.log("was also a client side command");
 *   }
 *
 *   // put it straight back
 *   manager.reRegisterCommand(node);
 * }
 * </pre>
 *
 * @since 1.6.5
 */
@DocletCategory("Commands")
public class CommandNodeHelper extends BaseHelper<CommandNode<?>> {
    /**
     * the node taken out of the mod loader's client-side command dispatcher, or {@code null}.
     * <p>
     * This is the second of the two places a command name can be registered, the first being
     * {@code getRaw()}. It is {@code null} whenever the name was not registered client-side,
     * which includes every node the runtime wrapped on its own rather than one the command
     * manager removed, and it is guarded by a null check rather than being a usable node.
     * Nothing in the game itself reads this field; the only reader anywhere is this mod's own
     * command manager, in its re-registration call, and it puts the node back only when this is
     * set.
     * example:
     * <pre>
     * const node = Chat.getCommandManager().unregisterCommand("hello");
     * if (node !== null) {
     *   // null means the name was not a client side command
     *   Chat.log(node.fabric === null ? "not a client side command" : "client side too");
     * }
     * </pre>
     */
    public final CommandNode<?> fabric;

    /**
     * wraps a node that was taken out of the server's command dispatcher.
     * <p>
     * This is the constructor the runtime uses when it wraps a raw command node a script-facing
     * call handed back. There is no client-side dispatcher to pair it with, so {@link #fabric}
     * is always {@code null} afterwards.
     *
     * @param base the node, which may itself be {@code null}
     */
    public CommandNodeHelper(CommandNode<?> base) {
        super(base);
        fabric = null;
    }

    /**
     * wraps a node that was taken out of both dispatchers.
     * <p>
     * This is the constructor the command manager uses, and it is the only one that fills
     * {@link #fabric}. Either argument may be {@code null}, since a command name may have been
     * registered in only one of the two places; the manager only creates this when at least one
     * of them was found.
     *
     * @param base the node taken out of the server's command dispatcher, or {@code null}
     * @param fabric the node taken out of the client-side command dispatcher, or {@code null}
     */
    public CommandNodeHelper(CommandNode<?> base, CommandNode<?> fabric) {
        super(base);
        this.fabric = fabric;
    }

}
