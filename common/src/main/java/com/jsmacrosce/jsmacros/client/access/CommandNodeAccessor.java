package com.jsmacrosce.jsmacros.client.access;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Reflectively removes direct children from Brigadier command nodes.
 * This accessor depends on reflective access to the node's child maps, which is
 * not guaranteed to be available on every version.
 * <pre>
 * const CommandNodes = Java.type("com.jsmacrosce.jsmacros.client.access.CommandNodeAccessor");
 * function removeChild(parent, name) {
 *   return CommandNodes.remove(parent, name);
 * }
 * </pre>
 */
public class CommandNodeAccessor {
    static Field children;
    static Field literals;
    static Field arguments;

    static {
        Field uf = null;
        try {
            children = CommandNode.class.getDeclaredField("children");
            children.setAccessible(true);
            literals = CommandNode.class.getDeclaredField("literals");
            literals.setAccessible(true);
            arguments = CommandNode.class.getDeclaredField("arguments");
            arguments.setAccessible(true);
        } catch (SecurityException ex) {
            throw new RuntimeException("I knew I should've just used unsafe...", ex);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Removes a direct child by its key from the parent's child map and, for
     * literal or argument nodes, the corresponding subtype map. Removal is not
     * recursive; failure accessing a subtype map can occur after the child has
     * already been removed from the child map.
     * @param <S> command-source type of the nodes
     * @param parent node whose direct-child maps are modified
     * @param name key of the child to remove
     * @return removed child, or {@code null} if no child has that key
     */
    public static <S> CommandNode<S> remove(CommandNode<S> parent, String name) throws IllegalAccessException {
        Map<String, CommandNode> c = (Map<String, CommandNode>) children.get(parent);
        CommandNode<S> node = c.remove(name);
        if (node == null) {
            return null;
        }
        if (node instanceof LiteralCommandNode<S>) {
            Map<String, LiteralCommandNode<S>> l = (Map<String, LiteralCommandNode<S>>) literals.get(parent);
            l.remove(name);
        } else if (node instanceof ArgumentCommandNode<S, ?>) {
            Map<String, ArgumentCommandNode<S, ?>> a = (Map<String, ArgumentCommandNode<S, ?>>) arguments.get(parent);
            a.remove(name);
        }
        return node;
    }

}
