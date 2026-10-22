package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.InventoryMenu;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;

/**
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Inventory")
public class PlayerInventory extends RecipeInventory<InventoryScreen> {

    protected PlayerInventory(InventoryScreen inventory) {
        super(inventory);
    }

    /**
     * the result slot, which is what the 2 by 2 grid in the player inventory is working towards.
     * <p>
     * The player inventory has a crafting grid of its own, so unlike a plain chest this is a
     * recipe inventory. This reads the slot rather than working out the recipe, so it is
     * whatever the client currently has there. The grid is read with
     * {@link #getInput(int, int)}, and note that {@link #getCraftingWidth()} reports 0 rather
     * than the grid's real width, so the two are worth telling apart.
     * <p>
     * It is slot 0 of the menu, the same as {@code getSlot(0)}, with the four grid slots after
     * it.
     * example:
     * <pre>
     * const inv = Player.openInventory();
     * if (inv.is("Survival Inventory")) {
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
        return new ItemStackHelper(inventory.getMenu().getResultSlot().getItem());
    }

    /**
     * @param x the x position of the input from 0 to 1, going left to right
     * @param y the y position of the input from 0 to 1, going top to bottom
     * @return the input item at the given position of the crafting grid.
     * @since 1.8.4
     */
    public ItemStackHelper getInput(int x, int y) {
        return getSlot(x + y * 2 + 1);
    }

    /**
     * always 0, which is not a statement about the grid.
     * <p>
     * The player inventory does have a 2 by 2 crafting grid, but this reports 0, so the number is
     * not measuring it. A script should not read the grid size off this: {@link #getInput(int,
     * int)} is the way in to the grid, and the reason for the 0 is not recorded in the code.
     * Neither does {@link #getInputSize()} help, because it works this and {@link
     * #getCraftingHeight()} out as a multiplication, so it comes back as 0 here too.
     * @return 0
     * @since 1.8.4
     */
    @Override
    public int getCraftingWidth() {
        return 0;
    }

    /**
     * always 0, which is not a statement about the grid.
     * <p>
     * The player inventory does have a 2 by 2 crafting grid, but this reports 0, so the number is
     * not measuring it, and the same goes for {@link #getCraftingWidth()}. A script should read
     * the grid with {@link #getInput(int, int)} rather than off this.
     * @return 0
     * @since 1.8.4
     */
    @Override
    public int getCraftingHeight() {
        return 0;
    }

    /**
     * always 0, which is not a statement about the grid.
     * <p>
     * Like {@link #getCraftingWidth()} and {@link #getCraftingHeight()}, this is fixed at 0 even
     * though the player inventory does have a crafting grid, and it is a literal rather than a
     * multiplication, so it happens to agree with {@link #getInputSize()} here. The consequence
     * is that {@link #getInput()} hands back a zero-length array instead of the four grid slots.
     * @return 0
     * @since 1.8.4
     */
    @Override
    public int getCraftingSlotCount() {
        return 0;
    }

    /**
     * @param slot the slot to check
     * @return {@code true} if the slot is in the hotbar or the offhand slot, {@code false}
     * otherwise.
     * @since 1.8.4
     */
    public boolean isInHotbar(int slot) {
        return InventoryMenu.isHotbarSlot(slot);
    }

    /**
     * @return the item in the offhand.
     * @since 1.8.4
     */
    public ItemStackHelper getOffhand() {
        return getSlot(45);
    }

    /**
     * @return the equipped helmet item.
     * @since 1.8.4
     */
    public ItemStackHelper getHelmet() {
        return getSlot(5);
    }

    /**
     * @return the equipped chestplate item.
     * @since 1.8.4
     */
    public ItemStackHelper getChestplate() {
        return getSlot(6);
    }

    /**
     * @return the equipped leggings item.
     * @since 1.8.4
     */
    public ItemStackHelper getLeggings() {
        return getSlot(7);
    }

    /**
     * @return the equipped boots item.
     * @since 1.8.4
     */
    public ItemStackHelper getBoots() {
        return getSlot(8);
    }

    @Override
    public String toString() {
        return String.format("PlayerInventory:{}");
    }

}
