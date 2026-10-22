package com.jsmacrosce.jsmacros.client.api.helper;

import com.jsmacrosce.doclet.DocletCategory;
import com.mojang.brigadier.tree.CommandNode;
import com.jsmacrosce.jsmacros.core.helpers.BaseHelper;

/**
 * Holds server- and client-side command nodes returned by unregistration for re-registration.
 * Either node may be {@code null} when that dispatcher had no matching command.
 * <pre>
 * const manager = Chat.getCommandManager();
 *
 * // take a command out, which gives back the node it was built from
 * const node = manager.unregisterCommand("hello");
 * if (node !== null) {
 *   // Either dispatcher may have had no matching command.
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
     * The node removed from the client-side command dispatcher, or {@code null} if none was removed.
     */
    public final CommandNode<?> fabric;

    /**
     * Wraps a server-side command node without a client-side node.
     *
     * @param base the server-side node, or {@code null}
     */
    public CommandNodeHelper(CommandNode<?> base) {
        super(base);
        fabric = null;
    }

    /**
     * Wraps the server- and client-side command nodes; either may be {@code null}.
     *
     * @param base the node taken out of the server's command dispatcher, or {@code null}
     * @param fabric the node taken out of the client-side command dispatcher, or {@code null}
     */
    public CommandNodeHelper(CommandNode<?> base, CommandNode<?> fabric) {
        super(base);
        this.fabric = fabric;
    }

}
