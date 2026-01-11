package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
public class CraftingInventory extends RecipeInventory<CraftingScreen> {

    protected CraftingInventory(CraftingScreen inventory) {
        super(inventory);
    }

    @Override
    public ItemStackHelper getOutput() {
        var handler = inventory.getMenu();
        handler.getResultSlot();
        return new ItemStackHelper(inventory.getMenu().getResultSlot().getItem());
    }

    /**
     * @param x the x position of the input from 0 to 2, going left to right
     * @param y the y position of the input from 0 to 2, going top to bottom
     * @return the input item at the given position.
     * @since 1.8.4
     */
    public ItemStackHelper getInput(int x, int y) {
        var handler = inventory.getMenu();
        return new ItemStackHelper(handler.getInputGridSlots().get(x + y * 3).getItem());
    }

    @Override
    public int getCraftingWidth() {
        return inventory.getMenu().getGridWidth();
    }

    @Override
    public int getCraftingHeight() {
        return inventory.getMenu().getGridHeight();
    }

    @Override
    public int getCraftingSlotCount() {
        return getCraftingWidth() * getCraftingHeight();
    }

    @Override
    public String toString() {
        return String.format("CraftingInventory:{}");
    }

}
