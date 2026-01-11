package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;
import com.jsmacrosce.jsmacros.client.mixin.access.MixinAnvilScreen;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@SuppressWarnings("unused")
public class AnvilInventory extends Inventory<AnvilScreen> {

    public AnvilInventory(AnvilScreen inventory) {
        super(inventory);
    }

    /**
     * @return the currently set name to be applied.
     * @since 1.8.4
     */
    public String getName() {
        return ((MixinAnvilScreen) inventory).getNameField().getValue();
    }

    /**
     * The change will be applied once the item is taken out of the anvil.
     *
     * @param name the new item name
     * @return self for chaining.
     * @since 1.8.4
     */
    public AnvilInventory setName(String name) {
        ((MixinAnvilScreen) inventory).getNameField().setValue(name);
        return this;
    }

    /**
     * @return the level cost to apply the changes.
     * @since 1.8.4
     */
    public int getLevelCost() {
        return inventory.getMenu().getCost();
    }

    /**
     * @return the amount of item needed to fully repair the item.
     * @since 1.8.4
     */
    public int getItemRepairCost() {
        return getSlot(0).getRepairCost();
    }

    /**
     * @return the maximum default level cost.
     * @since 1.8.4
     */
    public int getMaximumLevelCost() {
        return 40;
    }

    /**
     * @return the first input item.
     * @since 1.8.4
     */
    public ItemStackHelper getLeftInput() {
        return getSlot(0);
    }

    /**
     * @return the second input item.
     * @since 1.8.4
     */
    public ItemStackHelper getRightInput() {
        return getSlot(1);
    }

    /**
     * @return the expected output item.
     * @since 1.8.4
     */
    public ItemStackHelper getOutput() {
        return getSlot(2);
    }

    @Override
    public String toString() {
        return String.format("AnvilInventory:{}");
    }

}
