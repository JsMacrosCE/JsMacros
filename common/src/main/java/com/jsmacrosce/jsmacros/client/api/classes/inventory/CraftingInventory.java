package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;

/** Provides access to the input grid and result of a crafting menu.
 * <pre>
 * const inv = Player.openInventory();
 * if (inv instanceof Inventories.Crafting) {
 *   const cell = inv.getInput(0, 0);
 *   Chat.log(cell.isEmpty() ? "empty top-left cell" : cell.getItemId());
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Inventory")
public class CraftingInventory extends RecipeInventory<CraftingScreen> {

    protected CraftingInventory(CraftingScreen inventory) {
        super(inventory);
    }

    /**
     * Returns the current contents of the crafting menu's result slot; it does not calculate a
     * result independently.
     *
     * @return the item in the result slot
     * @since 1.8.4
     */
    @Override
    public ItemStackHelper getOutput() {
        var handler = inventory.getMenu();
        handler.getResultSlot();
        return new ItemStackHelper(inventory.getMenu().getResultSlot().getItem());
    }

    /**
     * Returns the input cell at the given column and row in the 3 by 3 grid.
     *
     * @param x column, from 0 (left) to 2 (right)
     * @param y row, from 0 (top) to 2 (bottom)
     * @return the item in that input slot
     * @since 1.8.4
     */
    public ItemStackHelper getInput(int x, int y) {
        var handler = inventory.getMenu();
        return new ItemStackHelper(handler.getInputGridSlots().get(x + y * 3).getItem());
    }

    /**
     * @return the crafting menu's grid width
     * @since 1.8.4
     */
    @Override
    public int getCraftingWidth() {
        return inventory.getMenu().getGridWidth();
    }

    /**
     * @return the crafting menu's grid height
     * @since 1.8.4
     */
    @Override
    public int getCraftingHeight() {
        return inventory.getMenu().getGridHeight();
    }

    /**
     * @return the number of input slots in the grid, excluding the result slot and player inventory
     * @since 1.8.4
     */
    @Override
    public int getCraftingSlotCount() {
        return getCraftingWidth() * getCraftingHeight();
    }

    /** Returns the fixed diagnostic label {@code CraftingInventory:{}}.
     *
     * @return the fixed diagnostic label {@code CraftingInventory:{}}
     */
    @Override
    public String toString() {
        return String.format("CraftingInventory:{}");
    }

}
