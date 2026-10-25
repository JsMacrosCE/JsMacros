package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.jsmacrosce.doclet.DocletCategory;

/**
 * Inventory handle for chest-like container screens. {@link #findFreeContainerSlot()} searches
 * the container section rather than the player's inventory.
 * <pre>
 * const inv = Player.openInventory();
 * if (inv instanceof Inventories.Container) {
 *   const free = inv.findFreeContainerSlot();
 *   Chat.log(free === -1 ? "the chest is full" : `first free slot is ${free}`);
 * }
 * </pre>
 * @param <T> screen type wrapped by this inventory
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Inventory")
@SuppressWarnings("unused")
public class ContainerInventory<T extends AbstractContainerScreen<?>> extends Inventory<T> {

    /**
     * Wraps the supplied container screen; this does not open a screen.
     * @param inventory screen to wrap, commonly obtained through {@link com.jsmacrosce.jsmacros.client.api.library.impl.FPlayer#openInventory()}
     */
    public ContainerInventory(T inventory) {
        super(inventory);
    }

    /**
     * @return the first empty menu slot in the container section, or {@code -1} if none is empty
     * @since 1.8.4
     */
    public int findFreeContainerSlot() {
        return findFreeSlot("container");
    }

    /** Returns this inventory's fixed diagnostic label.
     * @return {@code "ContainerInventory:{}"}
     */
    @Override
    public String toString() {
        return String.format("ContainerInventory:{}");
    }

}
