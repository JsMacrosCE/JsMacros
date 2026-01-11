package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class ContainerInventory<T extends AbstractContainerScreen<?>> extends Inventory<T> {

    public ContainerInventory(T inventory) {
        super(inventory);
    }

    /**
     * @return the first free slot in this container.
     * @since 1.8.4
     */
    public int findFreeContainerSlot() {
        return findFreeSlot("container");
    }

    @Override
    public String toString() {
        return String.format("ContainerInventory:{}");
    }

}
