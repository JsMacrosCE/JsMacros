package com.jsmacrosce.jsmacros.client.api.classes.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import com.jsmacrosce.doclet.DocletCategory;
import com.jsmacrosce.jsmacros.client.api.helper.inventory.ItemStackHelper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles an open stonecutter screen. Recipe indices in {@link #getRecipes()} correspond to
 * those accepted by {@link #selectRecipe(int)}.
 * <pre>
 * const inv = Player.openInventory();
 * if (inv.is("Stonecutter")) {
 *   if (inv.canCraft()) {
 *     const count = inv.getAvailableRecipeCount();
 *     Chat.log(`${count} cuts from ${inv.getSlot(0).getItemId()}`);
 *     inv.selectRecipe(0);
 *     Chat.log(`now making ${inv.getOutput().getItemId()}`);
 *   }
 * }
 * </pre>
 *
 * @author Etheradon
 * @since 1.8.4
 */
@DocletCategory("Inventory")
@SuppressWarnings("unused")
public class StoneCutterInventory extends Inventory<StonecutterScreen> {

    /**
     * Wraps the supplied stonecutter screen and captures its menu and current client player state.
     * Obtain the current inventory wrapper with {@link com.jsmacrosce.jsmacros.client.api.library.impl.FPlayer#openInventory()}.
     *
     * @param inventory the stonecutter screen to wrap
     */
    public StoneCutterInventory(StonecutterScreen inventory) {
        super(inventory);
    }

    /**
     * Returns the selected recipe index, or {@code -1} when no recipe is selected.
     *
     * @return the selected recipe index, or {@code -1} when none is selected
     * @since 1.8.4
     */
    public int getSelectedRecipeIndex() {
        return inventory.getMenu().getSelectedRecipeIndex();
    }

    /**
     * Returns the stonecutter result slot (slot 1).
     *
     * @return the result slot
     * @since 1.8.4
     */
    public ItemStackHelper getOutput() {
        return getSlot(1);
    }

    /**
     * Selects a currently visible recipe by its zero-based index. Indices outside the visible
     * range do nothing.
     *
     * @param idx zero-based recipe index
     * @return this inventory for chaining, regardless of whether selection occurred
     * @since 1.8.4
     */
    public StoneCutterInventory selectRecipe(int idx) {
        if (idx >= 0 && idx < inventory.getMenu().getNumberOfVisibleRecipes()) {
            inventory.getMenu().clickMenuButton(mc.player, idx);
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(getCurrentSyncId(), idx);
        }
        return this;
    }

    /**
     * Returns the number of currently visible recipes.
     *
     * @return the number of currently visible recipes
     * @since 1.8.4
     */
    public int getAvailableRecipeCount() {
        return inventory.getMenu().getNumberOfVisibleRecipes();
    }

    /**
     * Returns displayed results for the currently visible recipes, in selection-index order.
     *
     * @return displayed results in selection-index order
     * @since 1.8.4
     */
    public List<ItemStackHelper> getRecipes() {
        var context = SlotDisplayContext.fromLevel(mc.level);
        return inventory.getMenu().getVisibleRecipes().entries().stream().map(recipe ->
                new ItemStackHelper(recipe.recipe().optionDisplay().resolveForFirstStack(context))
                ).collect(Collectors.toList());
    }

    /**
     * Returns whether the menu has an input item.
     *
     * @return {@code true} if the menu has an input item; {@code false} otherwise
     * @since 1.8.4
     */
    public boolean canCraft() {
        return inventory.getMenu().hasInputItem();
    }

    /**
     * Returns the fixed diagnostic label {@code StoneCutterInventory:{}}.
     *
     * @return {@code StoneCutterInventory:{}}
     */
    @Override
    public String toString() {
        return String.format("StoneCutterInventory:{}");
    }

}
