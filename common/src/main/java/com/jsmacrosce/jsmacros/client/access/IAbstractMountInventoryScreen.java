package com.jsmacrosce.jsmacros.client.access;

import net.minecraft.world.entity.Entity;

/**
 * Mixin-backed access to the mount entity stored by supported mount inventory screens; this is
 * not a constructible screen type or a wrapper around the entity.
 * <pre>
 * if (Player.getPlayer()) {
 *   const screen = Player.openInventory().getRawContainer();
 *   if (screen) {
 *     if (typeof screen.jsmacros_getEntity === "function") {
 *       Chat.log(screen.jsmacros_getEntity());
 *     }
 *   }
 * }
 * </pre>
 */
public interface IAbstractMountInventoryScreen {
    /**
     * Returns the raw mount entity reference stored by this screen, without copying, wrapping,
     * creating, or selecting an entity. The concrete entity type depends on the supported screen
     * and Minecraft version.
     *
     * @return the raw mount entity stored by the screen
     */
    Entity jsmacros_getEntity();

}
