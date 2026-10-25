package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.InventoryMenu;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;

/** The player's inventory screen, including its crafting result, input grid, equipment, and hotbar.
 * <pre>
 * const inv = Player.openInventory();
 * if (inv instanceof Inventories.Player) {
 *   Chat.log(`${inv.getOutput().getItemId()} in the crafting result, `
 *     + `${inv.getOffhand().getItemId()} in the offhand`);
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Inventory")
public class PlayerInventory extends RecipeInventory<InventoryScreen> {

    protected PlayerInventory(InventoryScreen inventory) {
        super(inventory);
    }

    /**
     * Returns the item currently in the player's crafting result slot. Read the 2-by-2 input
     * grid with {@link #getInput(int, int)}; the crafting dimensions reported by this class are
     * zero.
     *
     * @return the current crafting result
     * @since 1.8.4
     */
    @Override
    public ItemStackHelper getOutput() {
        return new ItemStackHelper(inventory.getMenu().getResultSlot().getItem());
    }

    /**
     * Returns the item in the player's 2-by-2 crafting grid. Coordinates are zero-based, with
     * {@code x} increasing left to right and {@code y} top to bottom.
     *
     * @param x column in the grid, from 0 to 1
     * @param y row in the grid, from 0 to 1
     * @return the item in the selected grid slot
     * @since 1.8.4
     */
    public ItemStackHelper getInput(int x, int y) {
        return getSlot(x + y * 2 + 1);
    }

    /**
     * Returns 0; this value does not describe the player's 2-by-2 crafting grid. Use
     * {@link #getInput(int, int)} to read its cells.
     * @since 1.8.4
     */
    @Override
    public int getCraftingWidth() {
        return 0;
    }

    /**
     * Returns 0; this value does not describe the player's 2-by-2 crafting grid. Use
     * {@link #getInput(int, int)} to read its cells.
     * @since 1.8.4
     */
    @Override
    public int getCraftingHeight() {
        return 0;
    }

    /**
     * Returns 0 despite the four crafting cells; {@link #getInput()} therefore returns an
     * empty grid.
     * @since 1.8.4
     */
    @Override
    public int getCraftingSlotCount() {
        return 0;
    }

    /**
     * Checks whether a player-inventory menu slot is in the hotbar or offhand.
     *
     * @param slot menu slot index
     * @return {@code true} for hotbar slots 36 through 44 or offhand slot 45; {@code false}
     * otherwise
     * @since 1.8.4
     */
    public boolean isInHotbar(int slot) {
        return InventoryMenu.isHotbarSlot(slot);
    }

    /**
     * @return the item in menu slot 45 (the offhand)
     * @since 1.8.4
     */
    public ItemStackHelper getOffhand() {
        return getSlot(45);
    }

    /**
     * @return the item in the helmet slot
     * @since 1.8.4
     */
    public ItemStackHelper getHelmet() {
        return getSlot(5);
    }

    /**
     * @return the item in the chestplate slot
     * @since 1.8.4
     */
    public ItemStackHelper getChestplate() {
        return getSlot(6);
    }

    /**
     * @return the item in the leggings slot
     * @since 1.8.4
     */
    public ItemStackHelper getLeggings() {
        return getSlot(7);
    }

    /**
     * @return the item in the boots slot
     * @since 1.8.4
     */
    public ItemStackHelper getBoots() {
        return getSlot(8);
    }

    /** Returns the fixed diagnostic label {@code PlayerInventory:{}}.
     *
     * @return the fixed diagnostic label {@code PlayerInventory:{}}
     */
    @Override
    public String toString() {
        return String.format("PlayerInventory:{}");
    }

}
