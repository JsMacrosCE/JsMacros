package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import com.jsmacrosce.doclet.DocletCategory;

/**
 * a plain storage container: a chest, a barrel, a hopper, a dispenser or a shulker box.
 * <p>
 * It adds almost nothing to the base class on purpose, because there is nothing specific
 * about a chest. What is worth having is {@link #findFreeContainerSlot()}, which finds a free
 * slot in the container itself rather than in the player inventory around it.
 * <p>
 * The four screen types that count as a container all land here, so {@link #isContainer()} on
 * this is always true and the class is worth checking for rather than by screen name.
 * example:
 * <pre>
 * const inv = Player.openInventory();
 * if (inv.is("3 Row Chest")) {
 *   const free = inv.findFreeContainerSlot();
 *   Chat.log(free === -1 ? "the chest is full" : `first free slot is ${free}`);
 * }
 * </pre>
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Inventory")
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
