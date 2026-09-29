package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Inventory")
public class CraftingInventory extends RecipeInventory<CraftingScreen> {

    protected CraftingInventory(CraftingScreen inventory) {
        super(inventory);
    }

    /**
     * the result slot, which is what the crafting table grid is working towards.
     * <p>
     * This reads the slot rather than working out the recipe, so it is whatever the client
     * currently has there. The grid it is worked out from is read with
     * {@link #getInput(int, int)}, and the recipes the table can make are the ones
     * {@link #getRecipes(boolean)} lists.
     * <p>
     * It is slot 0 of the menu, the same as {@code getSlot(0)}, so it is counted before the
     * grid slots rather than after them.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Crafting Table")) {
     *   const out = inv.getOutput();
     *   if (!out.isEmpty()) {
     *     Chat.log(`crafting into ${out.getName()}`);
     *   }
     * }
     * </pre>
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
     * @param x the x position of the input from 0 to 2, going left to right
     * @param y the y position of the input from 0 to 2, going top to bottom
     * @return the input item at the given position.
     * @since 1.8.4
     */
    public ItemStackHelper getInput(int x, int y) {
        var handler = inventory.getMenu();
        return new ItemStackHelper(handler.getInputGridSlots().get(x + y * 3).getItem());
    }

    /**
     * the width of the crafting grid, read from the table rather than fixed.
     * <p>
     * A crafting table is 3 by 3, so this is 3 here, and the height is the same. Unlike
     * {@code PlayerInventory}, this is measured from the menu, so it would follow a grid of a
     * different size if the menu had one.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Crafting Table")) {
     *   Chat.log(`the grid is ${inv.getCraftingWidth()} by ${inv.getCraftingHeight()}`);
     * }
     * </pre>
     * @return the width of the grid, which is 3 for a crafting table
     * @since 1.8.4
     */
    @Override
    public int getCraftingWidth() {
        return inventory.getMenu().getGridWidth();
    }

    /**
     * the height of the crafting grid, read from the table rather than fixed.
     * <p>
     * A crafting table is 3 by 3, so this is 3 here, and the width is the same.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Crafting Table")) {
     *   Chat.log(`the grid is ${inv.getCraftingWidth()} by ${inv.getCraftingHeight()}`);
     * }
     * </pre>
     * @return the height of the grid, which is 3 for a crafting table
     * @since 1.8.4
     */
    @Override
    public int getCraftingHeight() {
        return inventory.getMenu().getGridHeight();
    }

    /**
     * the number of slots in the grid, which is the width times the height.
     * <p>
     * This is a count of input slots only, so it leaves out the result slot and the player
     * inventory around the table. It is also what {@link #getInputSize()} works out, so the two
     * agree.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Crafting Table")) {
     *   Chat.log(`${inv.getCraftingSlotCount()} grid slots`);
     * }
     * </pre>
     * @return the number of input slots in the grid
     * @since 1.8.4
     */
    @Override
    public int getCraftingSlotCount() {
        return getCraftingWidth() * getCraftingHeight();
    }

    @Override
    public String toString() {
        return String.format("CraftingInventory:{}");
    }

}
